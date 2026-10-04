from pathlib import Path
import json, subprocess, xml.etree.ElementTree as ET
root = Path(__file__).resolve().parents[3]
delivery = root / 'entrega/FASE12'
rows = []
for edition in ('FireTV','Shield'):
    project = root / ('DaNexus-' + edition)
    log = project / (edition.lower() + '-final-build.log')
    raw = log.read_bytes()
    text = raw.decode('utf-16') if raw.startswith((b'\xff\xfe',b'\xfe\xff')) else raw.decode('utf-8-sig',errors='replace')
    assert 'BUILD SUCCESSFUL' in text[-3000:], f'{edition} build did not finish successfully'
    assert ':app:testFullDebugUnitTest' in text and ':app:lintVitalFullDebug' in text
    suites = []
    for file in (project/'app/build/test-results/testFullDebugUnitTest').glob('TEST-*.xml'):
        x = ET.parse(file).getroot()
        suites.append({'name':x.attrib['name'],**{k:int(x.attrib[k]) for k in ('tests','failures','errors')}})
    assert sum(s['tests'] for s in suites) == 31
    assert all(s['failures']==0 and s['errors']==0 for s in suites)
    app_source = '52f46c9c9' if edition == 'FireTV' else '6591f0006'
    commit = subprocess.check_output(['git','-C',str(project),'rev-parse',app_source],text=True).strip()
    assert not subprocess.check_output(['git','-C',str(project),'diff',commit,'HEAD','--','app'],text=True).strip(), 'App source changed since compilation'
    apks = list((project/'app/build/outputs/apk/full/debug').glob('*.apk'))
    assert len(apks) == (5 if edition == 'FireTV' else 3)
    rows.append({'edition':edition,'exitCode':0,'log':str(log),'commit':commit,'apkCount':len(apks),'testSuites':suites,'statusRecoveredFrom':'successful build log and test XML after Windows sharing violation','appSourceIdentityVerified':True})
path = delivery/'BUILD_STATUS_FINAL.json'
path.write_text(json.dumps(rows,ensure_ascii=False,indent=2),encoding='utf-8')
print('Final records: 2 successful builds, 62 passed tests, 8 APKs')
