from zipfile import ZipFile
from lxml import etree as E
import json
ns={'w':'http://schemas.openxmlformats.org/wordprocessingml/2006/main'}
with ZipFile('C:/Users/LEGION/Videos/CHƯƠNG 1.docx') as z:
 b=E.fromstring(z.read('word/document.xml')).find('w:body',ns)
 for i in [301,302,303,304,527,528,529,531,534]:
  x=b[i];pr=x.find('w:pPr',ns)
  print(i,repr(''.join(x.xpath('.//w:t/text()',namespaces=ns))[:120]),E.tostring(pr,encoding='unicode').split('>')[1:] if pr is not None else None)
 s=E.fromstring(z.read('word/styles.xml'))
 for id in ['Normal','BodyText','Title','Heading1','Heading2','Caption','TableParagraph']:
  x=s.xpath('w:style[@w:styleId="'+id+'"]',namespaces=ns)
  if x: print('STYLE',id,E.tostring(x[0],encoding='unicode').split('>')[1:])
 print('docdefaults',E.tostring(s.find('w:docDefaults',ns),encoding='unicode').split('>')[1:])
