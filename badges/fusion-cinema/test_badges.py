import json,re,sys
from pathlib import Path
from PIL import Image
root=Path(__file__).parent
obj=json.loads((root/'badges.json').read_text(encoding='utf8'))
rows=obj['filters'];assert len(rows)==56,len(rows)
assert len({f['id'] for f in rows})==len(rows)
assert len({f['imageURL'] for f in rows})==len(rows)
for f in rows:
 assert f['isEnabled'] is True
 assert f['pattern'] and f['name'] and f['groupId']
 assert f['imageURL'].startswith('https://raw.githubusercontent.com/danilopagotto82/DaNexus-TV/')
 assert f['imageURL'].endswith('/png/'+f['id']+'.png')
 re.compile(f['pattern'])
 with Image.open(root/'png'/(f['id']+'.png')) as im:
  im.verify()
 with Image.open(root/'png'/(f['id']+'.png')) as im:
  assert im.size==(360,106) and im.mode=='RGBA'
by={f['id']:f for f in rows}
assert by['sf-51']['groupId']=='channels'
assert by['sf-atmos']['groupId']=='audio-object'
assert by['sf-truehd']['groupId']=='audio-codec'
assert by['sf-10bit']['groupId']=='video-depth'
assert re.search(by['sf-20']['pattern'],'stereo 2.0')
assert re.search(by['sf-4k']['pattern'],'2160p')
assert re.search(by['sf-dublado']['pattern'],'DUBLADO')
assert not re.search(by['sf-dublado']['pattern'],'LEGENDADO')
assert not re.search(by['sf-ptok']['pattern'],'Dublado')
assert re.search(by['sf-ptok']['pattern'],'Áudio em português confirmado')
print('PASS - 56 filtros, 56 logos, PNGs RGBA, regex, URLs e casos de idioma/áudio.')
