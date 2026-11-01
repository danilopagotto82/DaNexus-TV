# DaNEXUS Badge Fusion Cinema Edition – reproducible renderer
# Usage: py -3 generate.py ; requires Pillow. Reads CineBiel definitions.
import json, math, re, urllib.request
from collections import OrderedDict,Counter
from pathlib import Path
from PIL import Image,ImageDraw,ImageFont
ROOT=Path(__file__).resolve().parent
OUT=ROOT/'png'
OUT.mkdir(exist_ok=True)
SOURCE='https://cinebiel.duckdns.org/badges/badges.json'
BASE='https://raw.githubusercontent.com/danilopagotto82/DaNexus-TV/feature/badge-fusion-cinema-20261101/badges/fusion-cinema'
FONT=[r'C:\Windows\Fonts\bahnschrift.ttf',r'C:\Windows\Fonts\segoeuib.ttf',r'C:\Windows\Fonts\arialbd.ttf']
def font(n):
 for p in FONT:
  try:return ImageFont.truetype(p,n)
  except OSError:pass
 return ImageFont.load_default()
def color(h):return tuple(bytes.fromhex(h.strip('#')))
COL={'res':('EFC879','8F6A2D'),'hdr':('D8B5FF','70409B'),'src':('D6E7F5','59758A'),'codec':('83E5D9','286E73'),'audio':('CCAFFF','65539C'),'lang':('8BE8A7','236D4A'),'svc':('E9EDF6','657289'),'tag':('ECC780','9E7337'),'state':('9DE5C5','377A59')}
def shades(f):
 name=f['name'];grp=f['groupId'];c,b=COL.get(grp,COL['tag'])
 if grp=='res' and name!='4K':c,b='D9E2F2','66768D'
 if grp=='hdr' and name!='DV':c,b='FFC983','976130'
 if name=='REMUX':c,b='FFD785','A77727'
 if name=='BluRay':c,b='95DEFF','297A9C'
 if grp=='svc':
  pair={'Netflix':('FF7684','96283E'),'Prime Video':('8DE0FF','277F9F'),
  'Disney+':('9CB8FF','3F57A0'),'Max':('D5B2FF','7855A6'),
  'Crunchyroll':('FFBF7A','A96631'),'Hulu':('89F2AB','288353'),
  'Globoplay':('FFAD86','A34F3A')}.get(name)
  if pair:c,b=pair
 return color(c),color(b)
def label(name):
 return {'1080p':'FHD','720p':'HD','DV':'VISION','BluRay':'BLU-RAY',
 'WEBRip':'WEB RIP','ÁUDIO PT V':'AUDIO PT'}.get(name,name.upper())
def render(f):
 w,h=360,106;name=f['name'];grp=f['groupId'];c,b=shades(f)
 im=Image.new('RGBA',(w,h));d=ImageDraw.Draw(im)
 d.rounded_rectangle((5,5,w-6,h-6),25,fill=(9,14,24,255),outline=(*b,215),width=3)
 d.rounded_rectangle((9,9,w-10,45),18,fill=(23,27,39,90))
 d.arc((8,8,w-9,h-9),185,343,fill=(*c,200),width=3)
 d.rounded_rectangle((15,15,106,91),17,fill=(*b,55),outline=(*c,110),width=2)
 def ln(coords,width=5):d.line(coords,fill=(*c,255),width=width,joint='curve')
 def circ(box,fill=None,width=4):d.ellipse(box,fill=fill,outline=(*c,255),width=width)
 if name in ('DV','ATMOS','TrueHD','DD+','DD'):
  d.rounded_rectangle((28,32,54,75),6,outline=(*c,255),width=4)
  d.chord((41,32,77,75),90,270,fill=(*c,255))
  d.chord((41,32,77,75),270,90,fill=(*c,255))
 elif name.startswith('DTS'):
  d.polygon(((26,69),(47,35),(65,35),(44,69)),fill=(*c,255))
  d.polygon(((49,69),(70,35),(83,35),(61,69)),fill=(*c,255))
 elif grp=='res':
  d.rounded_rectangle((23,30,83,76),8,outline=(*c,255),width=4)
  ln((43,81,64,81),3)
 elif name=='REMUX':
  circ((24,22,86,84));circ((46,44,64,62),fill=(*c,255))
  for k in range(5):
   a=k*math.pi*2/5
   x=55+22*math.cos(a);y=53+22*math.sin(a)
   d.ellipse((x-5,y-5,x+5,y+5),fill=(*c,255))
 elif name in ('BluRay','DVD','CAM','HDTV'):
  circ((25,24,84,83));circ((49,48,62,61),fill=(*c,255))
 elif name in ('WEB-DL','WEBRip','WEB'):
  d.arc((30,26,82,76),175,355,fill=(*c,255),width=5)
  ln((56,44,56,80),5);ln((43,68,56,81,69,68),5)
 elif grp=='codec':
  d.rounded_rectangle((33,32,76,74),4,outline=(*c,255),width=4)
  d.rectangle((45,44,64,62),fill=(*c,255))
  for k in range(36,74,12):
   ln((k,22,k,30),3);ln((k,76,k,85),3)
 elif name=='DUBLADO' or name.startswith('ÁUDIO PT'):
  d.polygon(((55,22),(83,53),(55,85),(27,53)),fill='#27B45B')
  d.ellipse((39,38,70,69),fill='#F7CD52')
  d.arc((43,49,69,63),200,340,fill='#2555A6',width=4)
 elif name=='LEGENDADO':
  d.rounded_rectangle((24,28,84,76),10,outline=(*c,255),width=4)
  ln((37,43,71,43),4);ln((37,59,63,59),4)
 elif grp=='svc':
  if name=='Netflix':
   ln((41,29,41,78),10);ln((41,29,75,80),11);ln((75,29,75,78),10)
  elif name=='Disney+':
   d.arc((23,25,87,70),188,349,fill=(*c,255),width=4)
   circ((48,49,59,60),fill=(*c,255))
  elif name=='Prime Video':
   d.arc((23,39,87,82),15,160,fill=(*c,255),width=5)
   d.polygon(((86,61),(89,78),(72,71)),fill=(*c,255))
  elif name=='Crunchyroll':
   d.arc((27,22,82,82),40,310,fill=(*c,255),width=7)
  elif name=='Apple TV+':
   d.ellipse((34,43,77,81),fill=(*c,255));circ((47,30,67,47),fill=(*c,255))
   d.polygon(((68,42),(84,33),(76,51)),fill=(9,14,24))
  else:
   d.rounded_rectangle((30,30,82,79),12,outline=(*c,255),width=5)
   circ((48,47,62,61),fill=(*c,255))
 elif name in ('7.1','5.1','2.0'):
  circ((45,43,65,63),fill=(*c,255))
  d.arc((29,26,82,77),295,66,fill=(*c,255),width=5)
  d.arc((20,19,91,85),295,66,fill=(*c,255),width=3)
 elif grp=='tag':
  d.polygon(((55,23),(65,43),(86,46),(70,59),(74,82),(55,70),(37,82),(41,59),(24,46),(45,43)),outline=(*c,255))
 else:
  d.polygon(((55,20),(84,40),(84,67),(55,86),(26,67),(26,40)),outline=(*c,255))
  circ((45,44,65,64),fill=(*c,255))
 text=label(name);size=40 if len(text)<=5 else 32 if len(text)<=10 else 27
 fnt=font(size)
 while d.textbbox((0,0),text,font=fnt)[2]>207 and size>16:
  size-=2;fnt=font(size)
 box=d.textbbox((0,0),text,font=fnt)
 d.text((130,53-(box[3]-box[1])/2-box[1]),text,font=fnt,fill=(*c,255))
 d.line((132,82,210,82),fill=(*b,100),width=2)
 return im
def group(f):
 n=f['name'];g=f['groupId']
 if g=='audio':
  if n in ('7.1','5.1','2.0'):return 'channels'
  if n in ('ATMOS','DTS:X'):return 'audio-object'
  return 'audio-codec'
 if g=='hdr' and n=='10bit':return 'video-depth'
 if g=='state':return 'state-'+('direct' if n=='DIRETO' else 'language')
 return g
def main():
 with urllib.request.urlopen(SOURCE,timeout=20) as response:items=json.load(response)['filters']
 assert len(items)==56,('source changed',len(items))
 for f in items:
  if f['id']=='sf-20': f['pattern']=r'(?i)(?<!\d)2[.]0(?!\d)|\bstereo\b'
  if f['id']=='sf-ptok':
   f['name']='ÁUDIO PT'
   f['pattern']=r'(?i)(áudio em português confirmado|audio em portugues confirmado)'
 used=set();out=[]
 for f in items:
  slug=f['id'].lower();assert slug not in used;used.add(slug)
  p=OUT/(slug+'.png');render(f).save(p,optimize=True)
  entry={k:v for k,v in f.items() if k not in ('imageURL','tagColor','borderColor','textColor','tagStyle')}
  entry.update(groupId=group(f),imageURL=BASE+'/png/'+slug+'.png',
   tagColor='#00000000',borderColor='#00000000',textColor='#FFFFFFFF',tagStyle='')
  out.append(entry)
 (ROOT/'badges.json').write_text(json.dumps({'filters':out},ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
 ordered=OrderedDict()
 for f in items:ordered.setdefault(f['groupId'],[]).append(f)
 height=190+sum(50+math.ceil(len(v)/4)*118 for v in ordered.values())
 sheet=Image.new('RGB',(1570,height),(8,11,17));d=ImageDraw.Draw(sheet)
 d.text((65,31),'DaNEXUS   /   BADGE FUSION',font=font(51),fill='#EACB86')
 d.text((68,103),'CINEMA EDITION  •  LOGOS ORIGINAIS  •  SHIELD / FIRE TV',font=font(22),fill='#ADB9C8')
 y=175
 titles={'res':'RESOLUCAO','hdr':'HDR / IMAGEM','src':'FONTE / QUALIDADE','codec':'CODEC','audio':'AUDIO & CANAIS','lang':'IDIOMA','svc':'STREAMING','tag':'EDICOES ESPECIAIS','state':'ESTADO'}
 for grp,arr in ordered.items():
  d.text((67,y),titles.get(grp,grp.upper()),font=font(27),fill='#B7C5D7');y+=50
  for i,f in enumerate(arr):
   icon=render(f);sheet.paste(icon,(55+(i%4)*378,y+(i//4)*118),icon)
  y+=math.ceil(len(arr)/4)*118
 sheet.crop((0,0,1570,y+30)).save(ROOT/'preview.png',optimize=True)
 assert len(list(OUT.glob('*.png')))==56
 assert all((OUT/(f['id']+'.png')).exists() for f in out)
 assert len({f['groupId'] for f in out})>=10
 print('PASS: badges',len(out),'groups',dict(Counter(x['groupId'] for x in out)))
 print('PASS: pngs',len(list(OUT.glob('*.png'))),'preview',1570,'x',y+30)
if __name__=='__main__':main()
