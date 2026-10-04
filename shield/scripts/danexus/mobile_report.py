from pathlib import Path
import html, re

def inline(s):
    s = html.escape(s)
    return re.sub(r'\*\*(.+?)\*\*', r'<strong>\1</strong>', s)

def render_mobile_report(delivery, validated, partial=False):
    lines = (delivery / 'DaNexus-RELATORIO.md').read_text(encoding='utf-8').splitlines()
    body, list_open, table_open = [], False, False
    for line in lines + ['']:
        is_item = line.startswith('- ')
        is_table = line.startswith('|')
        if list_open and not is_item: body.append('</ul>'); list_open = False
        if table_open and not is_table: body.append('</tbody></table></div>'); table_open = False
        if is_item:
            if not list_open: body.append('<ul>'); list_open = True
            body.append('<li>' + inline(line[2:]) + '</li>')
        elif is_table:
            cells = [c.strip() for c in line.strip('|').split('|')]
            if all(re.fullmatch(r'[:\- ]+', c) for c in cells): continue
            if not table_open: body.append('<div class="table"><table><tbody>'); table_open = True
            body.append('<tr>' + ''.join('<td>' + inline(c) + '</td>' for c in cells) + '</tr>')
        elif line.startswith('#'):
            depth = min(3, len(line) - len(line.lstrip('#')))
            body.append(f'<h{depth}>' + inline(line.lstrip('# ').strip()) + f'</h{depth}>')
        elif line.strip(): body.append('<p>' + inline(line) + '</p>')
    checks = []
    for row in validated:
        count = sum(int(s['tests']) for s in row['tests'])
        checks.append(f'<article><h3>{html.escape(row["edition"])}</h3><p><strong>{count}/{count} testes aprovados</strong></p><ul><li>Compilação concluida</li><li>Assinatura, pacote e arquitetura conferidos</li><li>{len(row["variants"])} variantes de APK verificadas</li><li>Código-fonte e SHA-256 disponiveis</li></ul></article>')
    status = '<p class="status">Shield liberada; Fire em compilação. A página atualiza sozinha.</p>' if partial else '<p class="status">As duas edições estão prontas para instalar e testar.</p>'
    refresh = '<meta http-equiv="refresh" content="30">' if partial else ''
    page = '''<!doctype html><html lang="pt-BR"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">''' + refresh + '''<title>DaNexus - Relatório e testes</title><style>body{margin:0;background:#101722;color:#edf3fb;font:17px/1.6 system-ui,sans-serif}main{max-width:850px;margin:auto;padding:22px 20px 110px}h1{font-size:29px;line-height:1.2}h2{font-size:24px;margin-top:30px}h3{font-size:21px}p,li{color:#cfdae8}li{margin:9px 0}a{color:#add3ff}.tag{font-size:13px;letter-spacing:1px;color:#9dc6ff}.status,article{background:#1b293b;border:1px solid #3d5879;border-radius:16px;padding:16px}.table{overflow-x:auto}table{border-collapse:collapse;min-width:580px}td{border:1px solid #3d5879;padding:12px;vertical-align:top}.dock{position:fixed;bottom:0;left:0;right:0;padding:14px;background:#101722ee;text-align:center}.button{display:inline-block;background:#a8d0ff;color:#102039;padding:12px 22px;border-radius:12px;text-decoration:none;font-weight:700}strong{color:#fff}</style></head><body><main><div class="tag">FASE 12 - DaNexus personalizado</div>''' + status + ''.join(body) + '<h2>Check dos arquivos liberados</h2>' + ''.join(checks) + '''<h2>Como testar nas TVs</h2><ol><li>Abra a página de downloads no celular, conectado a mesma rede do PC. Escolha Fire TV para o quarto e Shield para a sala.</li><li>Instale o APK indicado no aparelho correspondente. Confirme a navegação com as setas, OK e Voltar, incluindo a barra superior na Home.</li><li>Teste um vídeo que inicie rapidamente: o aviso deve desaparecer quando a imagem começar.</li><li>Em uma fonte lenta, aguarde 5 segundos. Confira o aviso a direita, a contagem e os botões Mudar fonte e Continuar aguardando. Voltar fecha o aviso; a troca automática permanece ativa.</li><li>Pause ou abra um menu: o tempo dessas interrupcoes não entra na contagem. Ao trocar de fonte durante o vídeo, confira a retomada do ponto em que estava.</li><li>Na Shield, teste um REMUX compatível com seu conjunto e confira áudio e imagem. No Fire, teste reprodução 4K e fluidez. Os formatos disponiveis dependem dos aparelhos e da ligação HDMI.</li></ol><p>Os testes físicos de controle, reprodução, retomada, áudio e HDR ainda precisam ser feitos nas suas TVs. Os testes automatizados acima verificam a lógica do aplicativo.</p><p><a href="/DaNexus-RELATORIO.md">Baixar relatório em texto</a> &middot; <a href="/VALIDATION.json">Evidências da verificação</a></p></main><nav class="dock"><a class="button" href="/">Abrir downloads dos APKs</a></nav></body></html>'''
    target = delivery / 'DaNexus-RELATORIO.html'
    temporary = target.with_suffix('.html.tmp')
    temporary.write_text(page, encoding='utf-8')
    temporary.replace(target)
