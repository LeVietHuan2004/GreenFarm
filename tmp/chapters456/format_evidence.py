from pypdf import PdfReader
from pathlib import Path
from zipfile import ZipFile
from lxml import etree as E
p=Path('tmp/chapters456');pdf=PdfReader(p/'reference.pdf');print('Word pages',len(pdf.pages))
(p/'reference_render_text.txt').write_text('\n'.join(f'--- PAGE {i+1} ---\n'+x.extract_text() for i,x in enumerate(pdf.pages)),encoding='utf8')
ns={'w':'http://schemas.openxmlformats.org/wordprocessingml/2006/main'}
with ZipFile('C:/Users/LEGION/Videos/CHƯƠNG 1.docx') as z:
 b=E.fromstring(z.read('word/document.xml')).find('w:body',ns)
 for i in [75,76,77,157,301,302,303,304,527,528,529,531,534,535,548,549,550,551,552,553,554,555]:
  print(i,E.tostring(b[i],encoding='unicode')[-6000:])
 print('FIELDS',E.fromstring(z.read('word/document.xml')).xpath('//w:instrText/text()',namespaces=ns))
