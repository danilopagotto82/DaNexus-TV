from pathlib import Path
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import unquote, urlsplit
import json
root=Path(__file__).resolve().parents[3]/'entrega'/'FASE12'
root.mkdir(parents=True,exist_ok=True)
class Handler(SimpleHTTPRequestHandler):
 def __init__(self,*args,**kwargs): super().__init__(*args,directory=str(root),**kwargs)
 def allowed(self):
  p=unquote(urlsplit(self.path).path)
  if p in ('/','/index.html'): return True
  if '/' in p.lstrip('/') or '..' in p: return False
  f=root/p.lstrip('/')
  if f.suffix in ('.apk','.zip'):
   try: rows=json.loads((root/'VALIDATION.json').read_text(encoding='utf-8'))
   except (OSError,ValueError): return False
   allowed_names={row.get(key) for row in rows for key in ('apk','source')} | {v['apk'] for row in rows for v in row.get('variants',[])}
   if f.name not in allowed_names: return False
  return f.is_file() and (f.name.casefold().startswith('danexus-') and f.suffix in ('.apk','.zip','.md','.html') or f.name in ('SHA256SUMS.txt','BUILD_STATUS.json','BUILD_STATUS_FINAL.json','VALIDATION.json'))
 def do_GET(self):
  if not self.allowed(): self.send_error(404); return
  super().do_GET()
 def do_HEAD(self):
  if not self.allowed(): self.send_error(404); return
  super().do_HEAD()
 def list_directory(self,path): self.send_error(404); return None
print('DaNexus delivery server: port 8798; only APKs, source ZIPs and build status',flush=True)
ThreadingHTTPServer(('0.0.0.0',8798),Handler).serve_forever()
