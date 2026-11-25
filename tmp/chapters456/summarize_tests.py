from pathlib import Path
from xml.etree import ElementTree as E
import json
r=[]
for p in sorted(Path('backend/target/surefire-reports').glob('TEST-*.xml')):
 x=E.parse(p).getroot();r.append({'name':x.attrib['name'].split('.')[-1],**{k:int(x.attrib[k]) for k in ['tests','failures','errors','skipped']},'reasons':[z.get('message') for z in x.findall('.//skipped')]})
Path('tmp/chapters456/test-summary.json').write_text(json.dumps(r,ensure_ascii=False,indent=2),encoding='utf8')
for x in r:print(x)
