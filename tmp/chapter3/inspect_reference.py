from pathlib import Path
from zipfile import ZipFile
from lxml import etree as E
import json,hashlib
from docx import Document
root=Path(__file__).parent
ref=Path(r'C:\Users\LEGION\Downloads\PhanVanTinh_22010122_in.docx')
ns={'w':'http://schemas.openxmlformats.org/wordprocessingml/2006/main','a':'http://schemas.openxmlformats.org/drawingml/2006/main','r':'http://schemas.openxmlformats.org/officeDocument/2006/relationships'}
with ZipFile(ref) as z:
 body=E.fromstring(z.read('word/document.xml')).find('w:body',ns)
 rels={x.get('Id'):x.get('Target') for x in E.fromstring(z.read('word/_rels/document.xml.rels'))}
 out=[]
 for i,el in enumerate(body):
  if not 558<=i<1089:continue
  if el.tag==f'{{{ns["w"]}}}tbl':
   out.append('\nTABLE '+str(i))
   for tr in el.findall('w:tr',ns):
    out.append(' | '.join(' '.join(''.join(c.xpath('.//w:t/text()',namespaces=ns)).split()) for c in tr.findall('w:tc',ns)))
  else:
   out.append(str(i)+' '+ ' '.join(''.join(el.xpath('.//w:t/text()',namespaces=ns)).split()))
  for rid in el.xpath('.//a:blip/@r:embed',namespaces=ns):
   target=rels[rid];dest=root/Path(target).name;dest.write_bytes(z.read('word/'+target));out.append('IMAGE '+str(dest))
 (root/'reference_chapter3.txt').write_text('\n'.join(out),encoding='utf8')
 inventory=[{'path':n,'size':len(z.read(n)),'sha256':hashlib.sha256(z.read(n)).hexdigest()} for n in z.namelist()]
 (root/'reference_package.json').write_text(json.dumps(inventory,indent=2),encoding='utf8')
 print('REFERENCE HASH',hashlib.sha256(ref.read_bytes()).hexdigest())
 print('SECTION XML')
 for s in body.xpath('.//w:sectPr',namespaces=ns):print(E.tostring(s,encoding='unicode'))
 for i in [558,559,560,561,562,564,579,580,626,627,628,629]:
  print('ELEMENT',i,E.tostring(body[i],encoding='unicode')[:6500])
doc=Document(ref)
for s in ['Normal','Body Text','Heading 1','Heading 2','List Paragraph','Table Paragraph']:
 try:print('STYLE',s,doc.styles[s].element.xml[:5000])
 except KeyError:pass
