from pathlib import Path
from zipfile import ZipFile,ZIP_DEFLATED
from copy import deepcopy,copy
from io import BytesIO
from lxml import etree as E
from docx import Document
from docx.shared import Pt,Cm,RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT,WD_CELL_VERTICAL_ALIGNMENT
from docx.enum.style import WD_STYLE_TYPE
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
import re,json,hashlib
P=Path(__file__).parent
REF=Path(r'C:/Users/LEGION/Downloads/PhanVanTinh_22010122_in.docx')
FINAL=Path('output/report/GreenFarm_Chuong_3.docx')
md=(P/'chapter_final.md').read_text(encoding='utf8')
assert hashlib.sha256(REF.read_bytes()).hexdigest()=='206519fb83722f480faab4337c2e6f4c3f0eb399a180caa5016f4a73eb038ede'
d=Document(REF);body=d.element.body
for c in list(body):body.remove(c)
sect=OxmlElement('w:sectPr')
for tag,attrs in [('pgSz',{'w':'11910','h':'16840'}),('pgMar',{'top':'1100','bottom':'880','left':'1440','right':'720','header':'0','footer':'681'}),('pgNumType',{'start':'1'}),('cols',{'num':'1'})]:
 e=OxmlElement('w:'+tag)
 for k,v in attrs.items():e.set(qn('w:'+k),v)
 sect.append(e)
body.append(sect)
if 'Title' not in d.styles:
 st=d.styles.add_style('Title',WD_STYLE_TYPE.PARAGRAPH);st.base_style=d.styles['Normal']
def setfont(run,size=13,bold=False,italic=False):
 run.font.name='Times New Roman';run.font.size=Pt(size);run.font.bold=bold;run.font.italic=italic;run.font.color.rgb=RGBColor(0,0,0)
 rpr=run._element.get_or_add_rPr();rf=rpr.find(qn('w:rFonts'))
 if rf is None:rf=OxmlElement('w:rFonts');rpr.insert(0,rf)
 for k in ['ascii','hAnsi','eastAsia','cs']:rf.set(qn('w:'+k),'Times New Roman')
 for k in ['asciiTheme','hAnsiTheme','eastAsiaTheme','cstheme']:rf.attrib.pop(qn('w:'+k),None)
def formatting(p,size=13,kind='body',bold=False,italic=False):
 pf=p.paragraph_format;pf.left_indent=Pt(0);pf.right_indent=Pt(0);pf.first_line_indent=Cm(.5) if kind=='body' else Pt(0)
 pf.space_before=Pt(0);pf.space_after=Pt(5);pf.line_spacing=1.5;pf.widow_control=True;pf.keep_together=False;pf.keep_with_next=False
 pf.alignment=WD_ALIGN_PARAGRAPH.JUSTIFY if kind=='body' else WD_ALIGN_PARAGRAPH.LEFT
 pr=p._p.get_or_add_pPr();num=OxmlElement('w:numPr');nid=OxmlElement('w:numId');nid.set(qn('w:val'),'0');num.append(nid);pr.append(num)
 if kind=='title':pf.alignment=WD_ALIGN_PARAGRAPH.CENTER;pf.space_after=Pt(12);pf.keep_with_next=True
 elif kind=='heading':pf.space_before=Pt(9);pf.space_after=Pt(5);pf.keep_with_next=True
 elif kind=='caption':pf.alignment=WD_ALIGN_PARAGRAPH.CENTER;pf.space_before=Pt(5);pf.space_after=Pt(6);pf.line_spacing=1.15
 elif kind=='cell':pf.line_spacing=1.05;pf.space_after=Pt(0);pf.space_before=Pt(0);pf.keep_together=True
 for r in p.runs:setfont(r,size,bold,italic)
def para(text,style='Body Text',kind='body',size=13,bold=False,italic=False):
 selected=next((s for s in d.styles if s.style_id.lower()==style.replace(' ','').lower()),None)
 p=d.add_paragraph(style=selected or style);p.add_run(text);formatting(p,size,kind,bold,italic);return p
def table(rows,title):
 n=len(rows[0]);isuc='Đặc tả use case' in title
 if n==2:widths=[2600,7150] if isuc else [2100,7650]
 elif n==3:widths=[2100,6150,1500] if 'API ' in title else [2300,3800,3650]
 elif n==4:widths=[1900,3100,1750,3000] if 'Từ điển' in title else [2000,3300,1700,2750]
 elif n==5:widths=[850,1700,1400,4800,1000] if 'chức năng của GreenFarm' in title else [900,1300,5700,900,950]
 else:widths=[int(9750/n)]*n
 if n==6:widths=[3350,1000,1300,1400,1300,1400]
 if 'phi chức năng' in title:widths=[1000,1600,6000,1150]
 elif 'tác nhân của hệ thống' in title:widths=[1050,2100,6600]
 elif 'Danh sách use case' in title:widths=[1100,6800,1850]
 elif 'Phân nhóm các bảng' in title:widths=[2400,850,6500]
 elif 'các thành phần triển khai' in title:widths=[1900,4800,3050]
 elif 'Quyết định kiến trúc' in title:widths=[2300,4450,3000]
 elif 'Nhóm mã lỗi' in title:widths=[850,3850,5050]
 elif 'Hợp đồng dữ liệu' in title:widths=[2600,3000,4150]
 elif 'Danh sách màn hình' in title:widths=[2000,3550,1450,2750]
 widths[-1]=9750-sum(widths[:-1])
 t=d.add_table(rows=1,cols=n);t.alignment=WD_TABLE_ALIGNMENT.CENTER;t.autofit=False
 pr=t._tbl.tblPr
 w=pr.find(qn('w:tblW'));w.set(qn('w:w'),'9750');w.set(qn('w:type'),'dxa')
 borders=OxmlElement('w:tblBorders')
 for k in ['top','left','bottom','right','insideH','insideV']:
  x=OxmlElement('w:'+k);x.set(qn('w:val'),'single');x.set(qn('w:sz'),'4');x.set(qn('w:color'),'000000');borders.append(x)
 pr.append(borders)
 margins=OxmlElement('w:tblCellMar')
 for k,val in [('top','50'),('bottom','50'),('left','85'),('right','85')]:
  x=OxmlElement('w:'+k);x.set(qn('w:w'),val);x.set(qn('w:type'),'dxa');margins.append(x)
 pr.append(margins)
 for grid,width in zip(t._tbl.tblGrid,widths):grid.set(qn('w:w'),str(width))
 for ri,row in enumerate(rows):
  cells=t.rows[0].cells if ri==0 else t.add_row().cells
  for ci,(c,txtv) in enumerate(zip(cells,row)):
   c.width=Pt(widths[ci]/20);c.vertical_alignment=WD_CELL_VERTICAL_ALIGNMENT.CENTER
   size=11.5 if n>=4 or 'API ' in title else 12
   p=c.paragraphs[0];p.style=d.styles['Table Paragraph'];p.add_run(txtv.replace('<br>','\n'))
   formatting(p,size,'cell',ri==0)
   if ri==0:p.paragraph_format.alignment=WD_ALIGN_PARAGRAPH.CENTER;p.paragraph_format.keep_with_next=True
   if isuc and ci==0:p.runs[0].font.bold=True
  trpr=t.rows[ri]._tr.get_or_add_trPr();trpr.append(OxmlElement('w:cantSplit'))
  if ri==0:trpr.append(OxmlElement('w:tblHeader'))
  if isuc:
   for c in cells:
    for p in c.paragraphs:p.paragraph_format.keep_with_next=(ri<len(rows)-1)
 return t

lines=md.splitlines();i=0;lastcap='';figs=0;tables=0
while i<len(lines):
 s=lines[i].strip()
 if not s:i+=1;continue
 if s.startswith('|'):
  rows=[]
  while i<len(lines) and lines[i].strip().startswith('|'):
   ln=lines[i].strip()
   if not re.match(r'^\|\s*:?-{3}',ln):rows.append([x.strip() for x in ln.strip('|').split('|')])
   i+=1
  table(rows,lastcap);tables+=1;continue
 if s.startswith('!['):
  f=re.search(r'\]\(([^)]+)\)',s)[1];p=d.add_paragraph();formatting(p,kind='figure');pf=p.paragraph_format;pf.alignment=WD_ALIGN_PARAGRAPH.CENTER;pf.line_spacing=1;pf.space_before=Pt(6);pf.space_after=Pt(0);pf.keep_with_next=True
  # Keep figures within one page with their caption; no raster enlargement beyond text width.
  p.add_run().add_picture(str(Path('output/report')/f),width=Cm(17.05));figs+=1
 elif s.startswith('# '):para(s[2:],'Title','title',15,True)
 elif s.startswith('## '):para(s[3:],'Heading 2','heading',13,True)
 elif s.startswith('### '):para(s[4:],'List Paragraph','heading',13,False)
 elif s.startswith('#### '):para(s[5:],'List Paragraph','heading',12.5,True)
 elif re.match(r'^(Bảng|Hình) 3\.\d+\.',s):
  p=para(s,kind='caption',size=12,italic=True);p.paragraph_format.keep_with_next=s.startswith('Bảng');lastcap=s
 else:para(s)
 i+=1

# Save only changed package parts, retaining all other source ZIP members byte for byte.
bio=BytesIO();d.save(bio);bio.seek(0)
with ZipFile(REF) as a,ZipFile(bio) as b:
 changed={'word/document.xml','word/_rels/document.xml.rels','[Content_Types].xml','docProps/core.xml'}
 # Add Title to original style set only if the source did not contain it.
 original_styles=E.fromstring(a.read('word/styles.xml'))
 title_found=original_styles.xpath("//*[local-name()='style' and @*[local-name()='styleId']='Title']")
 if not title_found:changed.add('word/styles.xml')
 replacements={n:b.read(n) for n in changed if n in b.namelist()}
 doc=E.fromstring(replacements['word/document.xml']);ns={'w':'http://schemas.openxmlformats.org/wordprocessingml/2006/main'}
 footerid='rIdGreenFarmFooter'
 sec=doc.find('w:body/w:sectPr',ns);fr=OxmlElement('w:footerReference');fr.set(qn('w:type'),'default');fr.set(qn('r:id'),footerid);sec.insert(0,fr)
 replacements['word/document.xml']=E.tostring(doc,xml_declaration=True,encoding='UTF-8',standalone=True)
 rel=E.fromstring(replacements['word/_rels/document.xml.rels']);new=E.SubElement(rel,'{http://schemas.openxmlformats.org/package/2006/relationships}Relationship');new.set('Id',footerid);new.set('Type','http://schemas.openxmlformats.org/officeDocument/2006/relationships/footer');new.set('Target','footerGreenFarm.xml')
 replacements['word/_rels/document.xml.rels']=E.tostring(rel,xml_declaration=True,encoding='UTF-8',standalone=True)
 footer='''<?xml version="1.0" encoding="UTF-8" standalone="yes"?><w:ftr xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:p><w:pPr><w:jc w:val="center"/><w:spacing w:before="0" w:after="0"/></w:pPr><w:r><w:rPr><w:rFonts w:ascii="Times New Roman" w:hAnsi="Times New Roman"/><w:sz w:val="24"/></w:rPr><w:fldChar w:fldCharType="begin"/></w:r><w:r><w:instrText xml:space="preserve"> PAGE </w:instrText></w:r><w:r><w:fldChar w:fldCharType="end"/></w:r></w:p></w:ftr>'''
 replacements['word/footerGreenFarm.xml']=footer.encode()
 ct=E.fromstring(replacements['[Content_Types].xml']);ov=E.SubElement(ct,'{http://schemas.openxmlformats.org/package/2006/content-types}Override');ov.set('PartName','/word/footerGreenFarm.xml');ov.set('ContentType','application/vnd.openxmlformats-officedocument.wordprocessingml.footer+xml')
 replacements['[Content_Types].xml']=E.tostring(ct,xml_declaration=True,encoding='UTF-8',standalone=True)
 core=E.fromstring(replacements['docProps/core.xml'])
 for e in list(core):
  local=E.QName(e).localname
  if local in ['creator','lastModifiedBy']:e.text='GreenFarm'
  elif local=='title':e.text='Chương 3 Phân tích thiết kế hệ thống GreenFarm'
  elif local in ['subject','description','keywords']:e.text=''
 replacements['docProps/core.xml']=E.tostring(core,xml_declaration=True,encoding='UTF-8',standalone=True)
 # Remove obsolete author comments from the new chapter's document body is implicit
 # in the complete replacement; unused preserved parts are not referenced by it.
 added={n:b.read(n) for n in b.namelist() if n not in a.namelist()}
 with ZipFile(FINAL,'w',ZIP_DEFLATED) as out:
  for info in a.infolist():out.writestr(copy(info),replacements.get(info.filename,a.read(info.filename)))
  for n,data in {**added,**replacements}.items():
   if n not in a.namelist():out.writestr(n,data)
 with ZipFile(FINAL) as z:
  preserved=[n for n in a.namelist() if n not in changed]
  assert all(a.read(n)==z.read(n) for n in preserved)
  proof={'preserved_parts':len(preserved),'changed_parts':sorted(changed),'added_parts':[n for n in z.namelist() if n not in a.namelist()],'tables':tables,'figures':figs}
  (P/'package_fidelity.json').write_text(json.dumps(proof,indent=2),encoding='utf8')
print(FINAL,'tables',tables,'figures',figs,'bytes',FINAL.stat().st_size)
