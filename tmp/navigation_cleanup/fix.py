from pathlib import Path
from zipfile import ZipFile
from lxml import etree as E
import hashlib,json
src=Path('output/report/GreenFarm_BaoCao_Chuong_1_6.docx');out=Path('output/report/GreenFarm_BaoCao_Chuong_1_6_Heading_Gon.docx')
ns={'w':'http://schemas.openxmlformats.org/wordprocessingml/2006/main'};W=ns['w']
with ZipFile(src) as a:
 root=E.fromstring(a.read('word/document.xml'));before=E.tostring(root,method='c14n');changed=[]
 for i,p in enumerate(root.findall('.//w:p',ns)):
  if ''.join(p.xpath('.//w:t/text()',namespaces=ns)).strip():continue
  lvl=p.find('w:pPr/w:outlineLvl',ns)
  if lvl is not None and int(lvl.get('{'+W+'}val'))<9:
   changed.append((i,lvl.get('{'+W+'}val')));lvl.set('{'+W+'}val','9')
 data=E.tostring(root,xml_declaration=True,encoding='UTF-8',standalone=True)
 with ZipFile(out,'w') as b:
  for info in a.infolist():b.writestr(info,data if info.filename=='word/document.xml' else a.read(info.filename))
 for i,old in changed:root.findall('.//w:p',ns)[i].find('w:pPr/w:outlineLvl',ns).set('{'+W+'}val',old)
 assert E.tostring(root,method='c14n')==before
with ZipFile(src) as a,ZipFile(out) as b:
 assert [n for n in a.namelist() if a.read(n)!=b.read(n)]==['word/document.xml']
 root=E.fromstring(b.read('word/document.xml'))
 assert not [p for p in root.findall('.//w:p',ns) if not ''.join(p.xpath('.//w:t/text()',namespaces=ns)).strip() and p.find('w:pPr/w:outlineLvl',ns) is not None and int(p.find('w:pPr/w:outlineLvl',ns).get('{'+W+'}val'))<9]
Path('tmp/navigation_cleanup/patch-report.json').write_text(json.dumps({'source_sha256':hashlib.sha256(src.read_bytes()).hexdigest(),'output':str(out.resolve()),'blank_headings_demoted':len(changed),'changed_paragraphs':changed,'only_change':'w:outlineLvl from 0 to 9 on empty paragraphs'},indent=2),encoding='utf8')
print('Blank headings cleaned:',len(changed));print(out.resolve())
