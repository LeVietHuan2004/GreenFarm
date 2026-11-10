from pathlib import Path
import re,math,html
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib.enums import TA_JUSTIFY,TA_CENTER
from reportlab.lib import colors
from reportlab.platypus import SimpleDocTemplate,Paragraph,Spacer,Table,TableStyle,KeepTogether,PageBreak
from reportlab.graphics.shapes import Drawing,Rect,Line,String,Polygon
from reportlab.graphics import renderPDF
import pymupdf
ROOT=Path('output/report');FIG=ROOT/'figures';FIG.mkdir(exist_ok=True)
for n,f in [('TimesVN','times.ttf'),('TimesVN-Bold','timesbd.ttf'),('TimesVN-Italic','timesi.ttf')]:
 pdfmetrics.registerFont(TTFont(n,'C:/Windows/Fonts/'+f))
pdfmetrics.registerFontFamily('TimesVN',normal='TimesVN',bold='TimesVN-Bold',italic='TimesVN-Italic',boldItalic='TimesVN-Bold')
W=453
styles={
 'body':ParagraphStyle('body',fontName='TimesVN',fontSize=13,leading=19.5,alignment=TA_JUSTIFY,firstLineIndent=22,spaceAfter=5),
 'h1':ParagraphStyle('h1',fontName='TimesVN-Bold',fontSize=15,leading=21,alignment=TA_CENTER,spaceAfter=14,keepWithNext=True),
 'h2':ParagraphStyle('h2',fontName='TimesVN-Bold',fontSize=13.5,leading=19,spaceBefore=10,spaceAfter=7,keepWithNext=True),
 'h3':ParagraphStyle('h3',fontName='TimesVN-Bold',fontSize=13,leading=18.5,spaceBefore=7,spaceAfter=4,keepWithNext=True),
 'cap':ParagraphStyle('cap',fontName='TimesVN-Italic',fontSize=12,leading=16,alignment=TA_CENTER,spaceBefore=5,spaceAfter=8,keepWithNext=False),
 'cell':ParagraphStyle('cell',fontName='TimesVN',fontSize=10.5,leading=13),
 'head':ParagraphStyle('head',fontName='TimesVN-Bold',fontSize=10.5,leading=13,alignment=TA_CENTER),
 'ref':ParagraphStyle('ref',fontName='TimesVN',fontSize=11.5,leading=16,spaceAfter=9,wordWrap='CJK')}
def txt(text):return html.escape(text)
def diagram(kind):
 d=Drawing(W,225)
 def box(x,y,w,h,lines):
  d.add(Rect(x,y,w,h,strokeColor=colors.black,strokeWidth=.7,fillColor=colors.white))
  lines=lines.split('\n')
  for i,s in enumerate(lines):d.add(String(x+w/2,y+h/2+(len(lines)-1)*6-i*12,s,fontName='TimesVN',fontSize=10.5,textAnchor='middle'))
 def arrow(x,y,X,Y,label=None):
  d.add(Line(x,y,X,Y,strokeWidth=.8))
  a=math.atan2(Y-y,X-x);size=5
  d.add(Polygon([X,Y,X-size*math.cos(a-.45),Y-size*math.sin(a-.45),X-size*math.cos(a+.45),Y-size*math.sin(a+.45)],fillColor=colors.black))
  if label:d.add(String((x+X)/2+4,(y+Y)/2+5,label,fontName='TimesVN',fontSize=9))
 if kind=='commerce':
  d.height=190
  for x,s in [(0,'Tìm và chọn\nsản phẩm'),(163,'Kiểm tra giỏ\nvà ưu đãi'),(326,'Tạo đơn và\ngiữ hàng')]:box(x,130,126,44,s)
  arrow(126,152,163,152);arrow(289,152,326,152)
  box(326,32,126,44,'Xử lý đơn\nvà giao hàng');box(163,32,126,44,'Hoàn tất hoặc\nxử lý ngoại lệ');box(0,32,126,44,'Phản hồi và\nchăm sóc sau mua')
  arrow(389,130,389,76);arrow(326,54,289,54);arrow(163,54,126,54)
  d.add(String(226,104,'Thanh toán được kiểm tra theo phương thức COD hoặc trực tuyến',fontName='TimesVN-Italic',fontSize=10,textAnchor='middle'))
 elif kind=='architecture':
  box(0,150,125,54,'Frontend\nNext.js và React');box(165,150,126,54,'Backend\nSpring Boot');box(330,150,122,54,'Cơ sở dữ liệu\nMySQL')
  arrow(125,185,165,185,'API');arrow(165,169,125,169);arrow(291,185,330,185);arrow(330,169,291,169)
  for x,s in [(0,'VNPAY và GHN'),(165,'Cloudinary và SMTP'),(330,'Gemini API')]:box(x,35,122 if x==330 else 126,44,s)
  arrow(228,150,228,79);arrow(210,150,63,79);arrow(246,150,391,79)
  d.add(String(226,8,'Các kết nối dịch vụ ngoài được điều phối tại backend',fontName='TimesVN-Italic',fontSize=10,textAnchor='middle'))
 elif kind=='auth':
  d.height=220
  box(0,151,125,50,'Người dùng\ngửi đăng nhập');box(165,151,126,50,'Backend kiểm tra\ntài khoản và mật khẩu');box(330,151,122,50,'Cấp access token\nvà refresh token')
  arrow(125,177,165,177);arrow(291,177,330,177)
  box(330,35,122,52,'Gửi yêu cầu\nkèm access token');box(165,35,126,52,'Xác minh token\nvà kiểm tra quyền');box(0,35,125,52,'Cho phép hoặc\ntừ chối yêu cầu')
  arrow(391,151,391,87);arrow(330,61,291,61);arrow(165,61,125,61)
  d.add(String(226,113,'Refresh token được dùng khi cần làm mới phiên theo chính sách',fontName='TimesVN-Italic',fontSize=10,textAnchor='middle'))
 elif kind=='ai':
  d.height=235
  box(0,169,125,47,'Câu hỏi và\nlịch sử giới hạn');box(164,169,126,47,'Lọc dữ liệu và\nkiểm tra phạm vi');box(328,169,124,47,'Catalog và\nchính sách công khai')
  arrow(125,192,164,192);arrow(328,192,290,192)
  box(164,88,126,45,'Backend tạo\nngữ cảnh tư vấn');arrow(227,169,227,133)
  box(0,7,125,46,'Gọi Gemini\nkhi có cấu hình');box(328,7,124,46,'Trả lời dự phòng\nkhi cần');box(164,7,126,46,'Kiểm soát phản hồi\nvà trả cho frontend')
  arrow(164,99,63,53);arrow(290,99,390,53);arrow(125,30,164,30);arrow(328,30,290,30)
 return d
for i,k in enumerate(['commerce','architecture','auth','ai'],1):
 d=diagram(k);renderPDF.drawToFile(d,str(Path('tmp/report')/f'fig{i}.pdf'))
 p=pymupdf.open('tmp/report/fig'+str(i)+'.pdf');p[0].get_pixmap(matrix=pymupdf.Matrix(2.5,2.5)).save(str(FIG/f'hinh-2-{i}.png'))
source=Path('tmp/report/authoring.md').read_text(encoding='utf-8-sig')
parts=[p.strip() for p in source.split('\n\n') if p.strip()]
story=[];tables=0
for j,p in enumerate(parts):
 if p.startswith('# '):story.append(Paragraph(txt(p[2:]),styles['h1']))
 elif p.startswith('## '):
  if 'Tài liệu tham khảo' in p:story.append(Spacer(1,12))
  story.append(Paragraph(txt(p[3:]),styles['h2']))
 elif p.startswith('### '):story.append(Paragraph(txt(p[4:]),styles['h3']))
 elif p.startswith('[FIGURE:'):
  kind=p.split(':')[1][:-1];story.append(KeepTogether([diagram(kind),Paragraph(txt(parts[j+1]),styles['cap'])]))
 elif p.startswith('Hình 2.'):continue
 elif p.startswith('Bảng 2.'):continue
 elif p.startswith('|'):
  rows=[[s.strip() for s in line.strip().strip('|').split('|')] for line in p.splitlines()]
  rows=[row for row in rows if not all(re.fullmatch('[-: ]+',c) for c in row)]
  widths=[100,183,170] if tables==3 else ([68,159,226] if tables==1 else [110,170,173])
  t=Table([[Paragraph(txt(c),styles['head' if n==0 else 'cell']) for c in row] for n,row in enumerate(rows)],colWidths=widths,repeatRows=1,hAlign='CENTER')
  t.setStyle(TableStyle([('GRID',(0,0),(-1,-1),.5,colors.black),('VALIGN',(0,0),(-1,-1),'TOP'),('LEFTPADDING',(0,0),(-1,-1),5),('RIGHTPADDING',(0,0),(-1,-1),5),('TOPPADDING',(0,0),(-1,-1),5),('BOTTOMPADDING',(0,0),(-1,-1),5)]))
  capstyle=ParagraphStyle('tablecap',parent=styles['cap'],keepWithNext=True);cap=Paragraph(txt(parts[j-1]),capstyle);story.extend([cap,t,Spacer(1,8)]);tables+=1
 else:story.append(Paragraph(txt(p),styles['ref' if re.match(r'^\[\d+\]',p) else 'body']))
def page(c,doc):
 c.setFont('TimesVN',11);c.drawCentredString(306,29,str(doc.page))
 c.setTitle('GreenFarm Chương 2 Cơ sở lý thuyết và công nghệ');c.setAuthor('')
doc=SimpleDocTemplate(str(ROOT/'GreenFarm_Chuong_2.pdf'),pagesize=(595.28,841.89),leftMargin=85,rightMargin=57.28,topMargin=56.7,bottomMargin=55)
doc.build(story,onFirstPage=page,onLaterPages=page)
for i,k in enumerate(['commerce','architecture','auth','ai'],1):source=source.replace('[FIGURE:'+k+']',f'![Sơ đồ GreenFarm {i}](figures/hinh-2-{i}.png)')
(ROOT/'GreenFarm_Chuong_2.md').write_text(source,encoding='utf-8')
pdf=pymupdf.open(str(ROOT/'GreenFarm_Chuong_2.pdf'))
for i,p in enumerate(pdf):p.get_pixmap(matrix=pymupdf.Matrix(1.2,1.2)).save(f'tmp/report/page-{i+1}.png')
print('Pages:',len(pdf),'Tables:',tables,'Figures:4')
for i,p in enumerate(pdf):print(i+1,len(p.get_text()))
