from zipfile import ZipFile
from lxml import etree as E
from pathlib import Path
ns={'w':'http://schemas.openxmlformats.org/wordprocessingml/2006/main'};W=ns['w'];p=Path('output/report/GreenFarm_BaoCao_Chuong_1_6.docx')
with ZipFile(p) as z:
 root=E.fromstring(z.read('word/document.xml'));styles=E.fromstring(z.read('word/styles.xml'));sm={s.get('{'+W+'}styleId'):s for s in styles.findall('w:style',ns)}
 def slvl(s,seen=None):
  if not s or s not in sm:return None
  seen=set() if seen is None else seen
  if s in seen:return None
  seen.add(s);st=sm[s];level=st.find('w:pPr/w:outlineLvl',ns)
  if level is not None:return int(level.get('{'+W+'}val'))
  base=st.find('w:basedOn',ns)
  return slvl(base.get('{'+W+'}val'),seen) if base is not None else None
 def level(p):
  x=p.find('w:pPr/w:outlineLvl',ns)
  if x is not None:return int(x.get('{'+W+'}val'))
  s=p.find('w:pPr/w:pStyle',ns)
  return slvl(s.get('{'+W+'}val')) if s is not None else None
 rows=[];blank=[]
 for i,x in enumerate(root.findall('.//w:p',ns)):
  text=''.join(x.xpath('.//w:t/text()',namespaces=ns));lvl=level(x)
  if lvl is not None and lvl<9:
   record={'paragraph':i,'level':lvl,'text':text[:140],'drawing':bool(x.xpath('.//w:drawing|.//w:pict',namespaces=ns)),'page_break':bool(x.xpath('.//w:br[@w:type="page"]',namespaces=ns)),'style':x.xpath('w:pPr/w:pStyle/@w:val',namespaces=ns)}
   rows.append(record)
   if not text.strip():blank.append(record)
 print('Total outline entries',len(rows),'blank entries',len(blank))
 for x in blank:print(x)
 import json
 Path('tmp/navigation_cleanup/heading-audit.json').write_text(json.dumps(rows,ensure_ascii=False,indent=2),encoding='utf8')
