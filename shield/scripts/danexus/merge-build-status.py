from pathlib import Path
import json, os, time
root = Path(__file__).resolve().parents[3]
delivery = root / 'entrega/FASE12'
path = delivery / 'BUILD_STATUS.json'
rows = json.loads(path.read_text(encoding='utf-8-sig'))
if isinstance(rows, dict): rows = [rows]
status = {row['edition']: row for row in rows if isinstance(row,dict) and 'edition' in row}
shield = json.loads((delivery/'checkpoints/shield-package-recovery/Shield_SUCCESS.json').read_text(encoding='utf-8-sig'))
assert shield['edition'] == 'Shield' and shield['exitCode'] == 0
status['Shield'] = shield
assert all(status.get(e,{}).get('exitCode') == 0 for e in ('FireTV','Shield')), 'Both final builds must pass'
temporary = path.with_suffix('.pending.json')
temporary.write_text(json.dumps(list(status.values()),ensure_ascii=False,indent=2),encoding='utf-8')
for attempt in range(20):
    try:
        os.replace(temporary,path)
        break
    except PermissionError:
        if attempt == 19: raise
        time.sleep(0.1)
print('Successful Fire and Shield records consolidated atomically')
