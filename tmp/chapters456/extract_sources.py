from pathlib import Path
from zipfile import ZipFile
from lxml import etree as E
import json,hashlib
from pypdf import PdfReader
out=Path('tmp/chapters456');ns={'w':'http://schemas.openxmlformats.org/wordprocessingml/2006/main'}
p=Path('C:/Users/LEGION/Videos/CHƯƠNG 1.docx')
with ZipFile(p) as z:
 root=E.fromstring(z.read('word/document.xml'));body=root.find('w:body',ns)
 lines=[]
 for i,x in enumerate(body):
  t=''.join(x.xpath('.//w:t/text()',namespaces=ns))
  st=x.find('w:pPr/w:pStyle',ns)
  lines.append(f'{i} [{st.get("{'+ns['w']+'}val") if st is not None else ""}] {t}')
 (out/'word.txt').write_text('\n'.join(lines),encoding='utf8')
 (out/'sections.xml').write_text('\n'.join(E.tostring(s,encoding='unicode') for s in root.findall('.//w:sectPr',ns)),encoding='utf8')
 (out/'source_inventory.json').write_text(json.dumps({n:hashlib.sha256(z.read(n)).hexdigest() for n in z.namelist()},indent=2),encoding='utf8')
 print('doc body',len(body),'hash',hashlib.sha256(p.read_bytes()).hexdigest())
 print('\n'.join(lines[:35]));print('\n'.join(lines[-35:]))
pdf=PdfReader('C:/Users/LEGION/Videos/PhanVanTinh_22010122_in.pdf').pages
(out/'pdf.txt').write_text('\n'.join(f'\n--- PAGE {i+1} ---\n'+p.extract_text() for i,p in enumerate(pdf)),encoding='utf8')
print('pdf pages',len(pdf))

