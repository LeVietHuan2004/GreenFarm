from pathlib import Path
from PIL import Image,ImageDraw
from pypdf import PdfReader
from zipfile import ZipFile
from lxml import etree as E
import sys,re,json,hashlib
p=Path(sys.argv[1]);pdf=PdfReader(p/'GreenFarm_Chuong_3.pdf')
summ=[]
for i,page in enumerate(pdf.pages):
 text=page.extract_text() or '';summ.append(f'{i+1}: {len(text)} chars | '+text[:100].replace('\n',' ')+' ... '+text[-100:].replace('\n',' '))
(p/'page_text_summary.txt').write_text('\n'.join(summ),encoding='utf8')
ims=sorted(p.glob('page-*.png'),key=lambda f:int(f.stem.split('-')[-1]))
for k in range(0,len(ims),4):
 canvas=Image.new('RGB',(1800,2580),'white');draw=ImageDraw.Draw(canvas)
 for j,f in enumerate(ims[k:k+4]):
  im=Image.open(f);im.thumbnail((895,1255));x=j%2*900;y=j//2*1290;canvas.paste(im,(x,y+22));draw.text((x+10,y+2),f.stem,fill='black')
 canvas.save(p/f'contact-{k//4+1}.jpg')
md=Path('tmp/chapter3/chapter_final.md').read_text(encoding='utf8')
with ZipFile('output/report/GreenFarm_Chuong_3.docx') as z:
 doc=E.fromstring(z.read('word/document.xml'));ns={'w':'http://schemas.openxmlformats.org/wordprocessingml/2006/main'}
 text=' '.join(doc.xpath('//w:t/text()',namespaces=ns))
 for term in ['TopikLearn','TOPIK','Firebase','Groq','học viên']:assert term not in text,term
 assert len(doc.xpath('//w:tbl',namespaces=ns))==49
 for name in ['Hình','Bảng']:
  nums=[int(x) for x in re.findall(name+r' 3\.(\d+)\.',text)]
  assert nums==list(range(1,(25 if name=='Hình' else 49)+1)),(name,nums)
 assert len(doc.xpath('//w:drawing',namespaces=ns))==25
 assert len(doc.xpath('//w:sectPr',namespaces=ns))==1
 tables=set()
 for f in Path('backend/src/main/resources/db/migration').glob('*.sql'):
  tables.update(re.findall(r'CREATE TABLE(?: IF NOT EXISTS)?\s+`?(\w+)',f.read_text(encoding='utf8'),re.I))
 assert len(tables)==36
 assert all(t in text for t in tables)
 assert all(f'UC-{i:02}' in text for i in range(1,23))
 checksum=hashlib.sha256(Path(r'C:/Users/LEGION/Downloads/PhanVanTinh_22010122_in.docx').read_bytes()).hexdigest()
 assert checksum=='206519fb83722f480faab4337c2e6f4c3f0eb399a180caa5016f4a73eb038ede'
 print('QA PASS:',len(pdf.pages),'pages, 49 tables, 25 figures, 22 UCs, 36 schema tables; reference unchanged')
 print('\n'.join(summ))
