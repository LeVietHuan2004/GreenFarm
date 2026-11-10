from pathlib import Path
import re,json
root=Path('backend/src/main/java/com/agri/ecommerce')
out=[]
for f in sorted((root/'controller').rglob('*.java')):
 t=f.read_text(encoding='utf8')
 base=re.search(r'@RequestMapping\("([^"]*)"\)',t)
 if not base:continue
 auth=re.findall(r'@PreAuthorize\("([^\n]+)"\)',t)
 items=[]
 for m in re.finditer(r'@(Get|Post|Put|Patch|Delete)Mapping(?:\(([^\n]*?)\))?',t):
  arg=m[2] or '';s=re.search(r'"([^"]*)"',arg)
  items.append({'method':m[1].upper(),'path':base[1]+(s[1] if s else '')})
 out.append({'file':str(f),'security':auth,'endpoints':items})
Path('tmp/chapter3/api_inventory.json').write_text(json.dumps(out,ensure_ascii=False,indent=2),encoding='utf8')
for x in out:
 print(Path(x['file']).stem, str(x['security']))
 for e in x['endpoints']:print(' ',e['method'],e['path'])
