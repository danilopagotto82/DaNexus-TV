from pathlib import Path
import xml.etree.ElementTree as ET, json, re
parent = Path(__file__).resolve().parents[3]
failed = False
for edition in ['FireTV', 'Shield']:
 root = parent / ('DaNexus-' + edition)
 def resources(folder):
  return {x.get('name'): x for f in folder.glob('*.xml') for x in ET.parse(f).getroot() if x.get('name') and x.tag in ('string', 'plurals', 'string-array')}
 en = resources(root / 'app/src/main/res/values')
 pt = resources(root / 'app/src/main/res/values-pt-rBR')
 missing = [k for k, x in en.items() if k not in pt and x.get('translatable') != 'false']
 extras = sorted(pt.keys() - en.keys())
 mismatches = []
 for k in en.keys() & pt.keys():
  a = re.findall(r'%\d*\$?[sdif]', ''.join(en[k].itertext()))
  b = re.findall(r'%\d*\$?[sdif]', ''.join(pt[k].itertext()))
  if sorted(a) != sorted(b): mismatches.append(k)
 print(edition, 'missing=', len(missing), 'extra translations=', len(extras), 'placeholder mismatches=', len(mismatches))
 print(json.dumps({'missing': missing, 'extraTranslations': extras, 'placeholders': mismatches}, ensure_ascii=True))
 failed |= bool(missing or extras or mismatches)
raise SystemExit(1 if failed else 0)
