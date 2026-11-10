from pathlib import Path
import re
from docx import Document
from docx.shared import Pt,Cm,Inches
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.enum.table import WD_TABLE_ALIGNMENT,WD_CELL_VERTICAL_ALIGNMENT
root=Path('output/report')
source=(root/'GreenFarm_Chuong_2.md').read_text(encoding='utf-8')
parts=[p.strip() for p in source.split('\n\n') if p.strip()]
d=Document();sec=d.sections[0]
sec.page_width=Cm(21);sec.page_height=Cm(29.7)
sec.left_margin=Cm(3);sec.right_margin=Cm(2);sec.top_margin=Cm(2);sec.bottom_margin=Cm(1.94)
sec.footer_distance=Cm(.65)
for name in ['Normal','Title','Heading 1','Heading 2','Caption']:
 st=d.styles[name];st.font.name='Times New Roman';st.font.color.rgb=__import__('docx').shared.RGBColor(0,0,0)
 rf=st.element.get_or_add_rPr().rFonts
 for a in ['asciiTheme','hAnsiTheme','eastAsiaTheme','cstheme']:rf.attrib.pop(qn('w:'+a),None)
 for a in ['ascii','hAnsi','eastAsia','cs']:rf.set(qn('w:'+a),'Times New Roman')
normal=d.styles['Normal'];normal.font.size=Pt(13)
f=normal.paragraph_format;f.alignment=WD_ALIGN_PARAGRAPH.JUSTIFY;f.line_spacing=Pt(19.5);f.space_after=Pt(5);f.first_line_indent=Pt(22);f.widow_control=True
for name,size,before,after in [('Title',15,0,14),('Heading 1',13.5,10,7),('Heading 2',13,7,4)]:
 st=d.styles[name];st.font.size=Pt(size);st.font.bold=True
 pf=st.paragraph_format;pf.first_line_indent=Pt(0);pf.line_spacing=Pt(21 if name=='Title' else 19);pf.space_before=Pt(before);pf.space_after=Pt(after);pf.keep_with_next=True
 if name=='Title':pf.alignment=WD_ALIGN_PARAGRAPH.CENTER
cap=d.styles['Caption'];cap.font.size=Pt(12);cap.font.italic=True;cap.font.bold=False
cap.paragraph_format.first_line_indent=Pt(0);cap.paragraph_format.alignment=WD_ALIGN_PARAGRAPH.CENTER;cap.paragraph_format.line_spacing=Pt(16);cap.paragraph_format.space_before=Pt(5);cap.paragraph_format.space_after=Pt(8)
# Remove decorative title borders if inherited by Word styles.
for st in d.styles:
 if st.type==1:
  pf=st.element.find(qn('w:pPr'))
  if pf is not None:
   for e in list(pf.findall(qn('w:pBdr'))):pf.remove(e)
tables=0;figs=0
for p in parts:
 if p.startswith('# '):d.add_paragraph(p[2:],'Title')
 elif p.startswith('## '):d.add_paragraph(p[3:],'Heading 1')
 elif p.startswith('### '):d.add_paragraph(p[4:],'Heading 2')
 elif p.startswith('!['):
  path=re.search(r'\]\(([^)]+)\)',p)[1]
  para=d.add_paragraph();para.paragraph_format.first_line_indent=Pt(0);para.paragraph_format.alignment=WD_ALIGN_PARAGRAPH.CENTER;para.paragraph_format.keep_with_next=True;para.paragraph_format.space_after=Pt(0);para.paragraph_format.line_spacing=1
  para.add_run().add_picture(str(root/path),width=Cm(16));figs+=1
 elif p.startswith('Hình 2.'):
  if len(p.split())<22:d.add_paragraph(p,'Caption')
  else:d.add_paragraph(p)
 elif p.startswith('Bảng 2.'):
  para=d.add_paragraph(p,'Caption');para.paragraph_format.keep_with_next=True
 elif p.startswith('|'):
  rows=[[c.strip() for c in line.strip().strip('|').split('|')] for line in p.splitlines()]
  rows=[r for r in rows if not all(re.fullmatch(r'[-: ]+',c) for c in r)]
  widths=[100,183,170] if tables==3 else ([68,159,226] if tables==1 else [110,170,173])
  t=d.add_table(rows=0,cols=3);t.alignment=WD_TABLE_ALIGNMENT.CENTER;t.autofit=False;t.style='Table Grid'
  for col,w in zip(t.columns,widths):col.width=Pt(w)
  for n,row in enumerate(rows):
   cells=t.add_row().cells
   for cell,w,text in zip(cells,widths,row):
    cell.width=Pt(w);cell.vertical_alignment=WD_CELL_VERTICAL_ALIGNMENT.TOP
    para=cell.paragraphs[0];pf=para.paragraph_format;pf.first_line_indent=Pt(0);pf.alignment=WD_ALIGN_PARAGRAPH.CENTER if n==0 else WD_ALIGN_PARAGRAPH.LEFT;pf.line_spacing=Pt(13);pf.space_before=Pt(4);pf.space_after=Pt(4);pf.keep_with_next=False
    run=para.add_run(text);run.font.size=Pt(10.5);run.bold=(n==0)
   trpr=t.rows[-1]._tr.get_or_add_trPr();cant=OxmlElement('w:cantSplit');trpr.append(cant)
   if n==0:
    repeat=OxmlElement('w:tblHeader');trpr.append(repeat)
  tables+=1
 else:
  para=d.add_paragraph(p)
  if re.match(r'^\[\d+\]',p):
   pf=para.paragraph_format;pf.first_line_indent=Pt(0);pf.alignment=WD_ALIGN_PARAGRAPH.LEFT;pf.line_spacing=Pt(16);pf.space_after=Pt(9)
   for run in para.runs:run.font.size=Pt(11.5)
foot=sec.footer.paragraphs[0];foot.alignment=WD_ALIGN_PARAGRAPH.CENTER;foot.paragraph_format.first_line_indent=Pt(0)
r=foot.add_run();r.font.size=Pt(11);fld=OxmlElement('w:fldSimple');fld.set(qn('w:instr'),'PAGE');r._r.addnext(fld)
d.core_properties.title='GreenFarm Chương 2 Cơ sở lý thuyết và công nghệ';d.core_properties.author='';d.core_properties.comments=''
assert tables==4 and figs==4
d.save(root/'GreenFarm_Chuong_2.docx')
print('Created DOCX with',tables,'editable tables and',figs,'figures')
