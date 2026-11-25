from pathlib import Path
from zipfile import ZipFile
from lxml import etree as E
import hashlib,json
src=Path('C:/Users/LEGION/Videos/CHƯƠNG 1.docx');out=Path('output/report/GreenFarm_BaoCao_Chuong_1_6.docx');ns={'w':'http://schemas.openxmlformats.org/wordprocessingml/2006/main'}
with ZipFile(src) as a,ZipFile(out) as b:
 assert [n for n in a.namelist() if a.read(n)!=b.read(n)]==['word/document.xml']
 aa=E.fromstring(a.read('word/document.xml')).find('w:body',ns);bb=E.fromstring(b.read('word/document.xml')).find('w:body',ns)
 assert all(E.tostring(x,method='c14n')==E.tostring(y,method='c14n') for x,y in zip(list(aa)[:-1],list(bb)[:len(aa)-1]))
 new=bb[len(aa)-1:-1];text='\n'.join(''.join(x.xpath('.//w:t/text()',namespaces=ns)) for x in new)
 for n in range(1,19):assert text.count('Hình 4.'+str(n)+'. ')==1
 for n in range(1,6):assert text.count('Bảng 5.'+str(n)+'. ')==1
 for n in range(1,24):assert 'TC'+str(n).zfill(2) in text
 assert text.count('[Chèn ảnh kết quả tại đây]')==18
 assert not any(s in text for s in ['TopikLearn','Firebase','TOPIK'])
 assert hashlib.sha256(src.read_bytes()).hexdigest()=='848c5114c4e98cbca86c09891da53615907441095cf4ad729f5ef1267a5ec75d'
 print('Final QA passed: package preservation, original body, 18 captions, 23 test cases, source unchanged.')
 print('Deliverable bytes:',out.stat().st_size)
