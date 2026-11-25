from pathlib import Path
from zipfile import ZipFile
from copy import deepcopy
from lxml import etree as E
import hashlib,json,re

ROOT=Path(__file__).parent
SRC=Path('C:/Users/LEGION/Videos/CHƯƠNG 1.docx')
OUT=Path('D:/GreenFarm/output/report/GreenFarm_BaoCao_Chuong_1_6.docx')
W='http://schemas.openxmlformats.org/wordprocessingml/2006/main'
ns={'w':W}
def el(tag,attrs=None):
    x=E.Element('{'+W+'}'+tag)
    for k,v in (attrs or {}).items():x.set('{'+W+'}'+k,str(v))
    return x
def add(parent,tag,attrs=None):
    x=el(tag,attrs);parent.append(x);return x

with ZipFile(SRC) as z:
    raw=z.read('word/document.xml');doc=E.fromstring(raw);body=doc.find('w:body',ns)
    original_children=[E.tostring(x,method='c14n') for x in body]
    source_hash=hashlib.sha256(SRC.read_bytes()).hexdigest()

    def paragraph(text,kind='p'):
        # Inherit source paragraph roles, then make new content formatting explicit.
        idx={'p':302,'chapter':301,'h':303,'s':304,'caption':531,'slot':531,'cell':302}[kind]
        x=el('p');pr=deepcopy(body[idx].find('w:pPr',ns));x.append(pr)
        for n in ['rPr','ind','jc','spacing','outlineLvl']:
            for node in list(pr.findall('w:'+n,ns)):pr.remove(node)
        size=26;bold=False;italic=False;align='both';before=0;after=100;line=360;indent=283
        if kind=='chapter':size=32;bold=True;align='center';after=240;indent=0;add(pr,'pageBreakBefore');add(pr,'outlineLvl',{'val':0})
        elif kind=='h':size=28;bold=True;align='left';before=180;after=100;indent=0;add(pr,'outlineLvl',{'val':1})
        elif kind=='s':bold=True;align='left';before=140;after=80;indent=0;add(pr,'outlineLvl',{'val':2})
        elif kind in ['caption','slot']:size=24;italic=True;align='center';line=276;before=60;after=100;indent=0
        elif kind=='cell':size=23;align='left';line=276;after=40;indent=0
        add(pr,'spacing',{'before':before,'after':after,'line':line,'lineRule':'auto'})
        add(pr,'ind',{'left':0,'right':0,'firstLine':indent});add(pr,'jc',{'val':align})
        # Avoid paragraph defaults or source heading style sizes overriding new runs.
        r=add(x,'r');rp=add(r,'rPr')
        add(rp,'rFonts',{'ascii':'Times New Roman','hAnsi':'Times New Roman','eastAsia':'Times New Roman','cs':'Times New Roman'})
        add(rp,'b',{'val':int(bold)});add(rp,'bCs',{'val':int(bold)});add(rp,'i',{'val':int(italic)});add(rp,'iCs',{'val':int(italic)})
        add(rp,'color',{'val':'000000'});add(rp,'sz',{'val':size});add(rp,'szCs',{'val':size});add(rp,'lang',{'val':'vi-VN'})
        t=add(r,'t');t.text=text
        return x

    new=[]
    def table(title,headers,widths,rows):
        p=paragraph(title,'caption');add(p.find('w:pPr',ns),'keepNext');new.append(p)
        tbl=el('tbl');pr=add(tbl,'tblPr');add(pr,'tblW',{'w':sum(widths),'type':'dxa'});add(pr,'jc',{'val':'center'});add(pr,'tblLayout',{'type':'fixed'})
        borders=add(pr,'tblBorders')
        for tag in ['top','left','bottom','right','insideH','insideV']:add(borders,tag,{'val':'single','sz':4,'color':'D9D9D9'})
        margin=add(pr,'tblCellMar')
        for tag in ['top','bottom']:add(margin,tag,{'w':75,'type':'dxa'})
        for tag in ['left','right']:add(margin,tag,{'w':90,'type':'dxa'})
        grid=add(tbl,'tblGrid')
        for width in widths:add(grid,'gridCol',{'w':width})
        for ri,row in enumerate([headers]+rows):
            tr=add(tbl,'tr');trpr=add(tr,'trPr');add(trpr,'cantSplit')
            if ri==0:add(trpr,'tblHeader')
            for ci,(txt,width) in enumerate(zip(row,widths)):
                tc=add(tr,'tc');tcp=add(tc,'tcPr');add(tcp,'tcW',{'w':width,'type':'dxa'});add(tcp,'vAlign',{'val':'center'})
                if ri==0:add(tcp,'shd',{'fill':'E7E6E6','val':'clear'})
                p=paragraph(str(txt),'cell')
                if ri==0:
                    for b in p.findall('w:r/w:rPr/w:b',ns)+p.findall('w:r/w:rPr/w:bCs',ns):b.set('{'+W+'}val','1')
                if width<=1000 or (title.startswith('Bảng 5.5') and ci>0):p.find('w:pPr/w:jc',ns).set('{'+W+'}val','center')
                tc.append(p)
        new.append(tbl)

    summary=json.loads((ROOT/'test-summary.json').read_text(encoding='utf8'))
    counts={x['name']:x for x in summary}
    groups=[
      ('Tài khoản JWT và CORS',['AuthServiceTest','LoginAttemptServiceTest','UserServiceTest','JwtServiceTest','CorsConfigTest']),
      ('Catalog giỏ và khách vãng lai',['ProductServiceTest','CartServiceTest','WishlistServiceTest','GuestCommerceServiceTest','CommerceValidationTest']),
      ('Đơn hàng và thanh toán',['OrderServiceTest','OrderLifecycleServiceTest','PaymentServiceTest']),
      ('Voucher và điểm',['CouponServiceTest','CouponEngineServiceTest','LoyaltyServiceTest']),
      ('Kho',['InventoryServiceTest']),
      ('Tích hợp GHN với mô phỏng',['GhnClientTest','GhnPropertiesTest','GhnShippingServiceTest','GhnShipmentServiceTest','GhnWebhookServiceTest']),
      ('Đánh giá hỗ trợ và chatbot',['ReviewServiceTest','ContactServiceTest','NotificationServiceTest','ChatbotServiceTest','GeminiChatClientTest']),
      ('Email ảnh và báo cáo',['InvoiceEmailServiceTest','InvoiceEmailSenderTest','ImageStorageServiceTest','BusinessReportServiceTest']),
      ('Tích hợp có điều kiện',['GhnMigrationMysqlTest','LoyaltyConcurrencyMysqlTest','CloudinaryImageStorageLiveTest'])
    ]
    covered=[n for _,names in groups for n in names]
    assert set(covered)==set(counts) and len(covered)==len(counts)
    for block in json.loads((ROOT/'content.json').read_text(encoding='utf8')):
        kind=block[0]
        if kind in ['chapter','h','s','p']:
            p=paragraph(block[1],kind)
            if kind=='p' and new and new[-1].tag=='{'+W+'}tbl':p.find('w:pPr/w:spacing',ns).set('{'+W+'}before','100')
            new.append(p)
        elif kind=='fig':
            slot=paragraph('[Chèn ảnh kết quả tại đây]','slot');add(slot.find('w:pPr',ns),'keepNext');new.extend([slot,paragraph(block[1],'caption')])
        elif kind=='table':table(*block[1:])
        elif kind=='test_summary':
            rows=[]
            for name,names in groups:
                total=sum(counts[n]['tests'] for n in names);skip=sum(counts[n]['skipped'] for n in names)
                rows.append([name,total,total-skip,skip])
            rows.append(['Tổng',161,158,3])
            table('Bảng 5.5. Kết quả kiểm thử backend ngày 23 tháng 11 năm 2026',['Nhóm kiểm thử','Số ca','Đạt','Bỏ qua'],[6060,1100,1100,1100],rows)
        elif kind=='frontend_result':
            p=paragraph('Lệnh npm run quality của frontend hoàn tất với mã thoát 0: ESLint và production build đều thành công. Đây là kết quả kiểm tra mã và đóng gói ứng dụng; chưa phải kết quả kiểm thử thao tác người dùng trên trình duyệt.')
            p.find('w:pPr/w:spacing',ns).set('{'+W+'}before','100');new.append(p)
    # All original content and section geometry remain untouched.
    for x in new:body.insert(len(body)-1,x)
    assert [E.tostring(x,method='c14n') for x in body[:len(original_children)-1]]==original_children[:-1]
    assert E.tostring(body[-1],method='c14n')==original_children[-1]
    data=E.tostring(doc,xml_declaration=True,encoding='UTF-8',standalone=True)
    OUT.parent.mkdir(parents=True,exist_ok=True)
    with ZipFile(OUT,'w') as out:
        for info in z.infolist():out.writestr(info,data if info.filename=='word/document.xml' else z.read(info.filename))

with ZipFile(SRC) as a,ZipFile(OUT) as b:
    changed=[n for n in a.namelist() if a.read(n)!=b.read(n)]
assert changed==['word/document.xml']
assert hashlib.sha256(SRC.read_bytes()).hexdigest()==source_hash
(ROOT/'fidelity.json').write_text(json.dumps({'source_sha256':source_hash,'changed_parts':changed,'preserved_source_body_children':len(original_children)-1,'new_elements':len(new),'output':str(OUT)},indent=2),encoding='utf8')
print(OUT)
print('Added elements',len(new),'changed parts',changed)
