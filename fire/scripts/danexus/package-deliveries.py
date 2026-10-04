from pathlib import Path
from mobile_report import render_mobile_report
import argparse, sys, hashlib, html, json, os, re, shutil, subprocess, xml.etree.ElementTree as ET
parser = argparse.ArgumentParser()
parser.add_argument("--edition", choices=("FireTV", "Shield"))
args = parser.parse_args()
selected = [args.edition] if args.edition else ["FireTV", "Shield"]
root = Path(__file__).resolve().parents[3]
delivery = root / 'entrega' / 'FASE12'
status_path = delivery / ('BUILD_STATUS_FINAL.json' if (delivery / 'BUILD_STATUS_FINAL.json').exists() else 'BUILD_STATUS.json')
rows = json.loads(status_path.read_text(encoding='utf-8-sig'))
if isinstance(rows, dict): rows = [rows]
rows = [row for row in rows if isinstance(row, dict) and 'edition' in row]
status = {row['edition']: row for row in rows}
assert all(status.get(e, {}).get('exitCode') == 0 for e in selected), 'Selected builds must pass first'
assert 'FireTV' not in selected or status['FireTV']['log'].endswith('firetv-final-build.log'), 'Final FireTV resource check is required'
sdk = Path(os.environ.get('LOCALAPPDATA', '')) / 'Android' / 'Sdk' / 'build-tools' / '36.0.0'
env = os.environ.copy()
env['JAVA_HOME'] = r'C:\Users\danil\.jdks\jbr-21.0.11'
env['PATH'] = env['JAVA_HOME'] + r'\bin;' + env['PATH']
translation = subprocess.run([sys.executable, str(Path(__file__).with_name('audit-editions.py'))], capture_output=True, text=True)
assert translation.returncode == 0, 'Translation audit failed'
validated = []
for edition, abi, package in [('FireTV', 'armeabi-v7a', 'com.danexus.tv.fire.debug'), ('Shield', 'arm64-v8a', 'com.danexus.tv.shield.debug')]:
    if edition not in selected: continue
    project = root / ('DaNexus-' + edition)
    apks = list(delivery.glob(f'DaNexus-{edition}-*{abi}*.apk'))
    assert len(apks) == 1, f'Expected one {edition} APK for {abi}'
    apk = apks[0]
    badging = subprocess.check_output([str(sdk / 'aapt.exe'), 'dump', 'badging', str(apk)], env=env, text=True, encoding='utf-8', errors='replace')
    assert f"package: name='{package}'" in badging, 'Wrong application package'
    assert f"native-code: '{abi}'" in badging, 'Wrong native ABI'
    signature = subprocess.check_output([str(sdk / 'apksigner.bat'), 'verify', '--print-certs', str(apk)], env=env, text=True, encoding='utf-8', errors='replace')
    resources = subprocess.check_output([str(sdk / 'aapt.exe'), 'dump', 'resources', str(apk)], env=env, text=True, encoding='utf-8', errors='replace')
    assert all('string/danexus_slow_source_' + key in resources for key in ('title','body','countdown','next','wait')), 'Slow-source prompt resources missing from APK'
    variants = []
    for candidate in sorted(delivery.glob(f'DaNexus-{edition}-*.apk')):
        metadata = subprocess.check_output([str(sdk / 'aapt.exe'), 'dump', 'badging', str(candidate)], env=env, text=True, encoding='utf-8', errors='replace')
        assert f"package: name='{package}'" in metadata, 'Wrong variant package'
        native = next(line for line in metadata.splitlines() if line.startswith('native-code:'))
        arches = re.findall(r"'([^']+)'", native)
        expected = next((a for a in ('arm64-v8a', 'armeabi-v7a', 'x86_64', 'x86') if f'-{a}-' in candidate.name), None)
        if expected: assert arches == [expected], 'Variant ABI mismatch'
        else: assert ('universal' in candidate.name or candidate.name.endswith('-app-full-debug.apk')) and set(arches) == {'arm64-v8a','armeabi-v7a','x86','x86_64'}, 'Universal ABI mismatch'
        cert = signature if candidate == apk else subprocess.check_output([str(sdk / 'apksigner.bat'), 'verify', '--print-certs', str(candidate)], env=env, text=True, encoding='utf-8', errors='replace')
        variants.append({'apk':candidate.name,'abis':arches,'bytes':candidate.stat().st_size,'sha256':hashlib.sha256(candidate.read_bytes()).hexdigest(),'signature':cert})
    suites = []
    for file in (project / 'app' / 'build' / 'test-results' / 'testFullDebugUnitTest').glob('TEST-*.xml'):
        suite = ET.parse(file).getroot()
        suites.append({k: suite.attrib[k] for k in ('name', 'tests', 'failures', 'errors')})
    assert sum(int(s['tests']) for s in suites) == 31, 'Expected the 31 focused tests including the slow-source prompt'
    assert all(int(s['failures']) == 0 and int(s['errors']) == 0 for s in suites), 'Tests failed'
    assert not subprocess.check_output(['git', '-C', str(project), 'status', '--porcelain'], text=True).strip(), 'Source tree must be committed'
    commit = subprocess.check_output(['git', '-C', str(project), 'rev-parse', 'HEAD'], text=True).strip()
    built_commit = status[edition].get('commit')
    assert built_commit, 'Build must record its source commit'
    assert not subprocess.check_output(['git', '-C', str(project), 'diff', built_commit, 'HEAD', '--', 'app'], text=True).strip(), 'App source changed since the recorded build; rebuild this edition'
    source = delivery / f'DaNexus-{edition}-source.zip'
    subprocess.run(['git', '-C', str(project), 'archive', '--format=zip', '-o', str(source), 'HEAD'], check=True)
    shutil.copyfile(project / 'scripts' / 'danexus' / 'RELATORIO_SIMPLES.md', delivery / 'DaNexus-RELATORIO.md')
    validated.append({'edition': edition, 'abi': abi, 'package': package, 'apk': apk.name, 'bytes': apk.stat().st_size, 'sha256': hashlib.sha256(apk.read_bytes()).hexdigest(), 'source': source.name, 'commit': commit, 'tests': suites, 'badging': [line for line in badging.splitlines() if line.startswith(('package:', 'application-label:', 'native-code:', 'sdkVersion:', 'targetSdkVersion:'))], 'signature': signature, 'variants': variants, 'translationAudit': 'passed', 'slowSourcePromptResources': 5})
status_path.write_text(json.dumps(list(status.values()), ensure_ascii=False, indent=2), encoding='utf-8')
(delivery / 'VALIDATION.json').write_text(json.dumps(validated, ensure_ascii=False, indent=2), encoding='utf-8')
report = delivery / 'DaNexus-RELATORIO.md'
report.write_text(report.read_text(encoding='utf-8') + (f'\n\nValidacao parcial: {args.edition} passou nos 31 testes; pacote, arquitetura, assinatura e SHA-256 conferidos. A outra edicao continua em compilacao. Testes nos aparelhos pendentes.\n' if args.edition else '\n## Validação final dos arquivos\n\nAs duas compilações terminaram com sucesso. Cada edição passou nos 31 testes focados, sem falhas. Pacote, arquitetura, assinatura e SHA-256 dos dois APKs recomendados foram conferidos. O arquivo VALIDATION.json registra a evidência e os commits do código-fonte. Testes físicos nos seus aparelhos continuam pendentes.\n'), encoding='utf-8')
render_mobile_report(delivery, validated, partial=bool(args.edition))
files = sorted([*delivery.glob('DaNexus-*.apk'), *delivery.glob('DaNexus-*.zip'), delivery / 'DaNexus-RELATORIO.md', delivery / 'DaNexus-RELATORIO.html'])
(delivery / 'SHA256SUMS.txt').write_text(''.join(hashlib.sha256(file.read_bytes()).hexdigest() + '  ' + file.name + '\n' for file in files), encoding='ascii')
cards = []
for row in validated:
    room = 'Quarto · Fire TV Stick 4K Max de 2ª geração' if row['edition'] == 'FireTV' else 'Sala · NVIDIA Shield Pro 2017'
    cards.append(f'<article><h2>DaNexus {row["edition"]}</h2><p>{room}</p><p>{row["abi"]} · {row["bytes"] / 1048576:.1f} MB · 31 testes aprovados</p><a class="download" href="/{html.escape(row["apk"])}">Baixar APK</a><p><a href="/{row["source"]}">Código-fonte GPL-3.0</a></p><small>SHA-256: {row["sha256"]}</small></article>')
other_links = '<details><summary>Outras arquiteturas e APK universal (verificados)</summary><ul>' + ''.join(f'<li><a href="/{html.escape(v["apk"])}">{html.escape(v["apk"])}</a> - {v["bytes"]/1048576:.1f} MB</li>' for row in validated for v in row['variants'] if v['apk'] != row['apk']) + '</ul></details>'
page = '''<!doctype html><html lang="pt-BR"><meta charset="utf-8"><meta http-equiv="refresh" content="30"><meta name="viewport" content="width=device-width,initial-scale=1"><title>DaNexus · FASE 12</title><style>body{margin:0;background:#111720;color:#eef3fa;font:17px system-ui,sans-serif}main{max-width:1000px;margin:auto;padding:36px 22px}h1{font-size:36px}h2{font-size:24px}p{line-height:1.5;color:#c4cfdf}.grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(300px,1fr));gap:22px}article{padding:24px;background:#1c2634;border:1px solid #34455a;border-radius:18px}a{color:#99c9ff}.download{display:inline-block;padding:14px 24px;background:#99c9ff;color:#101923;border-radius:10px;text-decoration:none;font-weight:700}small{display:block;font-size:12px;overflow-wrap:anywhere;color:#a8b7cb}footer{margin-top:28px}</style><main><h1>DaNexus</h1><p>FASE 12 · Duas edições para seus aparelhos. APKs de teste com assinatura de desenvolvimento; podem coexistir com o Nuvio.</p><section class="grid">''' + ''.join(cards) + '''</section><footer><p><a href="/DaNexus-RELATORIO.html">Relatório simples: o que mudou e o que temos a mais</a> · <a href="/SHA256SUMS.txt">SHA-256</a> · <a href="/VALIDATION.json">Validação</a></p><p>Instale a edição indicada para cada aparelho. Reprodução, controle remoto, áudio e HDR precisam de conferência na ligação HDMI real. O painel do celular está na tela Sobre do aplicativo.</p><p>Base Fire TV: Nuvio oficial 1.1.0-beta.3. Base Shield: Cxsmo/Yso. Créditos e licença preservados.</p></footer></main></html>'''
page = page.replace('</footer>', other_links + '</footer>')
if args.edition: page = page.replace('</footer>', '<p>A outra edicao esta em compilacao. Esta pagina atualiza a cada 30 segundos.</p></footer>')
(delivery / 'index.html').write_text(page, encoding='utf-8')
print(json.dumps([{k: row[k] for k in ('edition', 'apk', 'bytes', 'sha256', 'commit')} for row in validated], indent=2))
