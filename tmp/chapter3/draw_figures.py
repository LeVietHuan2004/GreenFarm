from pathlib import Path
from PIL import Image,ImageDraw,ImageFont
import math,json
OUT=Path('output/report/figures_chuong3');OUT.mkdir(parents=True,exist_ok=True)
FONT=Path('C:/Windows/Fonts/arial.ttf');BOLD=Path('C:/Windows/Fonts/arialbd.ttf')
def font(size=36,bold=False):return ImageFont.truetype(str(BOLD if bold else FONT),size)
def canvas(w=2000,h=1250):
 im=Image.new('RGB',(w,h),'white');return im,ImageDraw.Draw(im)
def wrap(d,text,width,f):
 lines=[]
 for para in text.split('\n'):
  line=''
  for word in para.split():
   trial=(line+' '+word).strip()
   if d.textlength(trial,font=f)>width and line:lines.append(line);line=word
   else:line=trial
  lines.append(line)
 return lines
def txt(d,xy,text,size=36,bold=False,width=None,anchor='mm'):
 f=font(size,bold);lines=wrap(d,text,width,f) if width else text.split('\n');step=size*1.28
 x,y=xy
 if anchor=='mm':y-=(len(lines)-1)*step/2
 for line in lines:
  d.text((x,y),line,font=f,fill='black',anchor=anchor);y+=step
 return len(lines)*step
def box(d,rect,text,size=36,bold=False,fill='white'):
 d.rectangle(rect,fill=fill,outline='black',width=3);x1,y1,x2,y2=rect
 txt(d,((x1+x2)/2,(y1+y2)/2),text,size,bold,x2-x1-30)
def line(d,points,dashed=False,width=3):
 if not dashed:d.line(points,fill='black',width=width);return
 for a,b in zip(points,points[1:]):
  dx=b[0]-a[0];dy=b[1]-a[1];dist=math.hypot(dx,dy)
  for s in range(0,int(dist),18):
   e=min(s+10,dist);d.line([(a[0]+dx*s/dist,a[1]+dy*s/dist),(a[0]+dx*e/dist,a[1]+dy*e/dist)],fill='black',width=width)
def arrow(d,a,b,label=None,dashed=False,size=32):
 line(d,[a,b],dashed);ang=math.atan2(b[1]-a[1],b[0]-a[0]);head=14
 d.polygon([b,(b[0]-head*math.cos(ang-.45),b[1]-head*math.sin(ang-.45)),(b[0]-head*math.cos(ang+.45),b[1]-head*math.sin(ang+.45))],fill='black')
 if label:
  txt(d,((a[0]+b[0])/2,(a[1]+b[1])/2-30),label,size,width=max(abs(b[0]-a[0])-25,200))
def actor(d,x,y,label,scale=1):
 r=18*scale;d.ellipse((x-r,y-r,x+r,y+r),outline='black',width=3)
 d.line((x,y+r,x,y+75*scale),fill='black',width=3)
 d.line((x-36*scale,y+42*scale,x+36*scale,y+42*scale),fill='black',width=3)
 d.line((x,y+75*scale,x-30*scale,y+113*scale),fill='black',width=3);d.line((x,y+75*scale,x+30*scale,y+113*scale),fill='black',width=3)
 txt(d,(x,y+150*scale),label,32,width=230)
def save(im,key):im.save(OUT/(key+'.png'),dpi=(250,250))

def context():
 im,d=canvas(2000,1270);box(d,(650,400,1350,850),'HỆ THỐNG GREENFARM\nMua sắm • đơn hàng • kho\nThanh toán • giao nhận • hỗ trợ',42,True)
 for i,t in enumerate(['Khách vãng lai','Khách hàng','Staff và giao hàng','Quản trị viên']):
  y=120+i*290;actor(d,150,y,t);arrow(d,(265,y+70),(650,480+i*90))
 for i,t in enumerate(['VNPAY\nThanh toán và hoàn tiền','GHN staging\nPhí và sự kiện vận đơn','SMTP và Cloudinary\nEmail và ảnh','Gemini\nTư vấn công khai']):
  y=60+i*290;box(d,(1570,y,1980,y+190),t,34);arrow(d,(1350,465+i*105),(1570,y+95))
 txt(d,(1000,1185),'Trình duyệt → GreenFarm API; khóa dịch vụ chỉ đặt tại backend',32,width=1600);save(im,'context')

def usecase(key,actors,cases,links,title):
 # cases contain label, column (0/1), row; each actor is at the corresponding side.
 rows=max([x[2] for x in cases])+1;h=max(1150,180+rows*190)
 im,d=canvas(2100,h);d.rectangle((325,40,1775,h-35),outline='black',width=3);txt(d,(1050,90),title,38,True)
 loc={}
 for i,(label,col,row) in enumerate(cases):
  x=750+col*630;y=240+row*190;loc[i]=(x,y)
  d.ellipse((x-255,y-62,x+255,y+62),outline='black',width=3);txt(d,(x,y),label,34,width=445)
 for i,(label,side,row) in enumerate(actors):
  x=155 if side=='L' else 1945;y=200+row*190;actor(d,x,y-45,label)
  for a,b in links:
   if a!=i:continue
   tx,ty=loc[b];sx=x+65 if side=='L' else x-65;end=tx-255 if side=='L' else tx+255
   cross=(side=='L' and tx>1050) or (side=='R' and tx<1050)
   if cross:
    corridor=290 if side=='L' else 1810
    line(d,[(sx,y+5),(corridor,y+5),(corridor,ty-95),(tx,ty-95),(tx,ty-62)])
   else:line(d,[(sx,y+5),(end,ty)])
 save(im,key)

def sequence(key,names,messages,note=None):
 w=2100;n=len(names);xs=[150+i*1800/(n-1) for i in range(n)];fs=32
 layout=[];y=205
 im0,d0=canvas()
 for a,b,t,*rest in messages:
  width=abs(xs[b]-xs[a])-35 if a!=b else 355
  width=max(width,280);lines=wrap(d0,t,width,font(fs));height=max(85,len(lines)*41+38)
  layout.append((a,b,t,rest,y,height));y+=height
 h=y+100+(120 if note else 0);im,d=canvas(w,h)
 for i,name in enumerate(names):
  box(d,(xs[i]-145,25,xs[i]+145,145),name,32,True)
  line(d,[(xs[i],145),(xs[i],h-60-(120 if note else 0))],True)
 for num,(a,b,t,rest,y,ht) in enumerate(layout,1):
  dashed=bool(rest and rest[0]);ya=y+ht-12
  if a==b:
   x=xs[a];line(d,[(x,ya-12),(x+90,ya-12),(x+90,ya+10),(x,ya+10)]);arrow(d,(x+50,ya+10),(x,ya+10))
   txt(d,(min(x+195,w-220),y+20),f'{num}. '+t,fs,width=350)
  else:
   txt(d,((xs[a]+xs[b])/2,y+(ht-45)/2),f'{num}. '+t,fs,width=max(abs(xs[b]-xs[a])-35,280))
   arrow(d,(xs[a],ya),(xs[b],ya),dashed=dashed)
 if note:box(d,(110,h-140,w-110,h-20),note,30,fill='#f6f6f6')
 save(im,key)

def architecture():
 im,d=canvas(2100,1440)
 box(d,(70,80,540,1320),'TRÌNH DUYỆT\n\nNext.js và React\n\nTrang cửa hàng\nPortal vận hành\n\nAxios / Query\nZustand / Form\n\nToken và trạng thái UI',36,True)
 d.rectangle((740,40,1490,1350),outline='black',width=3);txt(d,(1115,95),'SPRING BOOT API',40,True)
 blocks=[('JWT và Spring Security',170),('Controller và DTO validation',360),('Service nghiệp vụ\nĐơn • Kho • Coupon • Điểm',550),('Repository và JPA',790),('Scheduler và SSE\nEmail retry • Quét hết hạn',1030)]
 for t,y in blocks:box(d,(800,y,1430,y+150),t,34)
 for a,b in [(320,360),(510,550),(700,790)]:arrow(d,(1115,a),(1115,b))
 arrow(d,(540,470),(800,470),'HTTPS / JSON',size=30)
 box(d,(1680,80,2050,310),'MySQL\nDữ liệu quan hệ',36,True);arrow(d,(1430,865),(1680,195),'SQL / JDBC',size=28)
 box(d,(1680,460,2050,780),'Dịch vụ ngoài\nVNPAY • GHN\nSMTP • Cloudinary\nGemini',34)
 arrow(d,(1430,625),(1680,625),'API',size=28)
 arrow(d,(800,1105),(540,1105),'SSE',size=28)
 box(d,(1620,1010,2050,1290),'Lỗi chuẩn hóa\nTransaction\nHash và khóa dữ liệu',34);save(im,'architecture')

def deployment():
 im,d=canvas(2100,1320)
 box(d,(70,80,560,350),'Thiết bị người dùng\nTrình duyệt web',38,True)
 box(d,(840,70,1390,350),'VERCEL\nFrontend Next.js\nRoot frontend',38,True);arrow(d,(560,210),(840,210),'HTTPS tải web',size=30)
 d.rectangle((735,490,2030,1200),outline='black',width=3);txt(d,(1375,545),'RAILWAY',42,True)
 box(d,(800,650,1380,970),'Backend Docker\nSpring Boot / Java 21\nPORT • /api/health\nREST • SSE • Scheduler',36,True)
 box(d,(1580,650,1960,970),'MySQL\nVolume bền vững\n/var/lib/mysql',36,True)
 arrow(d,(1380,810),(1580,810),'JDBC',size=29)
 line(d,[(315,350),(315,810)]);arrow(d,(315,810),(800,810),'HTTPS API / SSE',size=30)
 box(d,(75,1040,590,1270),'Cloudinary\nẢnh qua URL HTTPS',36)
 arrow(d,(315,900),(315,1040))
 box(d,(1560,60,2040,360),'Dịch vụ hỗ trợ\nVNPAY sandbox\nGHN staging\nSMTP • Gemini',34)
 line(d,[(1800,360),(1800,450),(1450,450),(1450,710)]);arrow(d,(1450,710),(1380,710))
 txt(d,(1380,1110),'Bí mật tại backend • Callback công khai qua HTTPS',31,width=1120);save(im,'deployment')

def ai():
 im,d=canvas(2100,1190)
 labels=['Câu hỏi và phiên','Che dữ liệu nhạy cảm\nKiểm tra phạm vi','Catalog công khai\nLịch sử cùng chủ thể','Prompt tại backend\nGemini API','Lọc phản hồi\nĐối chiếu sản phẩm','Hiển thị tư vấn\nLưu lịch sử đã lọc']
 coords=[(100,90,640,280),(820,90,1360,280),(1540,90,2080,280),(1540,570,2080,790),(820,570,1360,790),(100,570,640,790)]
 for r,t in zip(coords,labels):box(d,r,t,35)
 for a,b in [((640,185),(820,185)),((1360,185),(1540,185)),((1810,280),(1810,570)),((1540,680),(1360,680)),((820,680),(640,680))]:arrow(d,a,b)
 box(d,(760,940,1570,1140),'Không có khóa hoặc Gemini lỗi\n→ trả lời dự phòng từ catalog và chính sách',34)
 line(d,[(1810,790),(1810,1040)]);arrow(d,(1810,1040),(1570,1040));line(d,[(760,1040),(370,1040)]);arrow(d,(370,1040),(370,790))
 txt(d,(1080,425),'Câu hỏi dữ liệu riêng hoặc thao tác giao dịch → từ chối',33,width=1650);save(im,'ai')

def states():
 im,d=canvas(2100,1100)
 positions={'pending':(100,170,500,330),'processing':(850,170,1250,330),'ready_for_delivery':(1600,170,2040,330),'out_for_delivery':(1600,540,2040,700),'delivered':(850,540,1250,700),'completed':(100,540,500,700),'delivery_failed':(1590,860,2050,1020),'canceled':(850,860,1250,1020)}
 for name,r in positions.items():box(d,r,name,32,True)
 for a,b in [((500,250),(850,250)),((1250,250),(1600,250)),((1820,330),(1820,540)),((1600,620),(1250,620)),((850,620),(500,620)),((1820,700),(1820,860)),((1590,940),(1250,940))]:arrow(d,a,b)
 line(d,[(2040,940),(2080,940),(2080,390),(1930,390)]);arrow(d,(1930,390),(1930,330))
 txt(d,(1640,795),'Giao thất bại',30)
 line(d,[(300,330),(300,425),(680,425),(680,940)]);arrow(d,(680,940),(850,940))
 line(d,[(1050,330),(1050,410),(710,410),(710,900)]);arrow(d,(710,900),(850,900))
 line(d,[(1710,330),(1710,445),(740,445),(740,980)]);arrow(d,(740,980),(850,980))
 txt(d,(1030,75),'Chuyển trạng thái có kiểm tra vai trò thanh toán và tồn kho',34,True,width=1850)
 txt(d,(430,805),'Hủy theo điều kiện; đơn đã trả tiền\nphải qua nghiệp vụ hoàn tiền',30,width=700);save(im,'order_states')

def erd(key,entities,edges,h=1550):
 h=55+max(x[4] for x in entities)*405+345+50
 im,d=canvas(2100,h);rects={}
 # entities: id,title,fields,col,row
 for eid,title,fields,col,row in entities:
  x=50+col*700;y=55+row*405;w=600;bh=345;rects[eid]=(x,y,x+w,y+bh)
 for a,b,card in edges:
  ra,rb=rects[a],rects[b];ca=((ra[0]+ra[2])/2,(ra[1]+ra[3])/2);cb=((rb[0]+rb[2])/2,(rb[1]+rb[3])/2)
  if abs(ca[0]-cb[0])>abs(ca[1]-cb[1]):
   start=(ra[2] if cb[0]>ca[0] else ra[0],ca[1]);end=(rb[0] if cb[0]>ca[0] else rb[2],cb[1]);mx=(start[0]+end[0])/2
   pts=[start,(mx,start[1]),(mx,end[1]),end]
  else:
   start=(ca[0],ra[3] if cb[1]>ca[1] else ra[1]);end=(cb[0],rb[1] if cb[1]>ca[1] else rb[3]);my=(start[1]+end[1])/2
   pts=[start,(start[0],my),(end[0],my),end]
  line(d,pts)
  short=card.replace(' — ',':').replace(' trong SQL','').replace(' qua dòng đơn','').replace('qua phân bổ','qua lô')
  if abs(ca[0]-cb[0])>abs(ca[1]-cb[1]):pos=((pts[-2][0]+end[0])/2,end[1]-32)
  else:pos=(end[0]+100,(pts[-2][1]+end[1])/2)
  bb=d.textbbox(pos,short,font=font(23),anchor='mm');d.rectangle((bb[0]-4,bb[1]-2,bb[2]+4,bb[3]+2),fill='white');txt(d,pos,short,23)
 for eid,title,fields,col,row in entities:
  r=rects[eid];d.rectangle(r,fill='white',outline='black',width=3);d.rectangle((r[0],r[1],r[2],r[1]+78),fill='#ededed',outline='black',width=3)
  txt(d,((r[0]+r[2])/2,r[1]+39),title,30 if len(title)>31 else 34,True)
  y=r[1]+105
  for f in fields:txt(d,(r[0]+20,y),f,34,anchor='la');y+=46
 save(im,key)

def concept():
 entities=[('u','Người dùng',['Vai trò và dữ liệu cá nhân'],0,0),('o','Đơn hàng',['Dòng hàng • thanh toán','Snapshot • lịch sử'],1,0),('p','Sản phẩm',['Danh mục • ảnh • giá'],2,0),('g','Phiên khách',['Giỏ khách • khóa checkout'],0,1),('c','Ưu đãi và hậu mãi',['Coupon • điểm • đánh giá','Liên hệ • thông báo'],1,1),('b','Kho và nguồn cung',['Lô • FEFO • phân bổ','Lịch sử • nhà cung cấp'],2,1)]
 erd('er_concept',entities,[('u','o','0..1 — N'),('g','o','0..1 — N'),('o','p','N — N qua dòng đơn'),('u','c','1 — N'),('p','b','1 — N'),('o','b','qua phân bổ')],1070)

def ers():
 erd('er_accounts',[
 ('role','roles',['PK id','name UNIQUE'],0,0),('user','users',['PK id; FK role_id','email UNIQUE','credential_version','loyalty_points_balance'],1,0),('sess','guest_sessions',['PK id; token_hash UNIQUE','FK merged_user_id (NULL)','expires_at; consumed_at'],2,0),
 ('perm','permissions',['PK id; name UNIQUE','N–N qua role_permissions','FK role_id, permission_id','UNIQUE cặp role / permission'],0,1),('tokens','Token và địa chỉ',['refresh_tokens: FK user_id','greenfarm_password_reset_tokens','shipping_addresses: FK user_id'],1,1),('gc','guest_cart_items',['PK id; FK guest_session_id','FK product_id; quantity','UNIQUE (session, product)'],2,1),
 ('cart','cart_items và wishlists',['PK id; FK user_id','FK product_id','UNIQUE (user, product)'],0,2),('pr','products',['PK id','slug UNIQUE'],1,2)],
 [('role','user','1 — N'),('role','perm','N — N'),('user','tokens','1 — N'),('sess','gc','1 — N'),('user','cart','1 — N'),('cart','pr','N — 1')],1510)
 erd('er_commerce',[
 ('cat','categories',['PK id','name và slug UNIQUE'],0,0),('p','products',['PK id; FK category_id','price; stock; status','origin; certification'],1,0),('img','product_images',['PK id; FK product_id','image URL'],2,0),
 ('u','users / guest_sessions',['PK id','Chủ tài khoản hoặc phiên khách'],0,1),('o','orders',['PK id; FK user_id (NULL)','FK guest_session_id (NULL)','subtotal; total_price; status','Snapshot địa chỉ / coupon'],1,1),('item','order_items',['PK id; FK order_id','FK product_id','quantity; price','Snapshot sản phẩm'],2,1),
 ('hist','order_status_history',['PK id; FK order_id','status; changed_at; note'],0,2),('pay','payments',['PK id; FK order_id','reference_code UNIQUE','amount; status; expires_at','order_id chưa UNIQUE'],1,2),('ref','refund_requests / vnpay_refunds',['PK id','FK order_id (refund_requests)','FK payment_id (vnpay_refunds)','request_id UNIQUE ở gateway'],2,2)],
 [('cat','p','1 — N'),('p','img','1 — N'),('u','o','0..1 — N'),('o','item','1 — N'),('p','item','1 — N'),('o','hist','1 — N'),('o','pay','1 — N trong SQL'),('pay','ref','1 — N')],1510)
 erd('er_inventory',[
 ('s','suppliers',['PK id; supplier_code UNIQUE','name; status'],0,0),('p','products',['PK id','stock đồng bộ khả dụng'],1,0),('user','users',['PK id','Người thao tác kho'],2,0),
 ('batch','inventory_batches',['PK id; FK product_id','FK supplier_id (NULL)','remaining_quantity','reserved_quantity; expiry_date'],0,1),('alloc','order_inventory_allocations',['PK id; FK order_item_id','FK batch_id; quantity','status; exported_at','restored_at'],1,1),('trans','inventory_transactions',['PK id; FK product_id','FK batch_id; FK created_by','quantity_before / after','reserved_before / after'],2,1),
 ('o','orders',['PK id','Trạng thái quyết định','giữ / xuất / hoàn'],0,2),('item','order_items',['PK id; FK order_id','FK product_id; quantity'],1,2)],
 [('s','batch','1 — 0..N'),('p','batch','1 — N'),('batch','alloc','1 — N'),('batch','trans','1 — N'),('user','trans','1 — N'),('item','alloc','1 — N'),('o','item','1 — N')],1510)
 erd('er_engagement',[
 ('coupon','coupons',['PK id; code UNIQUE','Loại / thời hạn / giới hạn','Phạm vi qua coupon_categories','và coupon_products'],0,0),('usage','coupon_usages',['PK id; FK coupon_id/order_id','FK user_id/guest_session_id','UNIQUE (coupon_id, order_id)','RESERVED / USED / RELEASED'],1,0),('order','orders',['PK id','FK coupon_id/shipping_coupon_id','FK user_id/guest_session_id'],2,0),
 ('user','users',['PK id','Sở hữu dữ liệu tài khoản'],0,1),('engage','Tương tác và điểm',['reviews: FK user_id/product_id','contacts: FK user/responded_by','notifications: FK user_id','loyalty_point_transactions','chat_messages: user/guest hash'],1,1),('ship','shipments',['PK id; FK order_id UNIQUE','client_order_code UNIQUE','provider_order_code UNIQUE','shipping_status; cod_amount'],2,1),
 ('event','shipment_events',['PK id; FK shipment_id','UNIQUE (shipment_id, event_key)','event_type; event_at'],2,2)],
 [('coupon','usage','1 — N'),('order','usage','1 — N'),('user','engage','1 — N'),('order','ship','1 — 0..1'),('ship','event','1 — N')],1510)

def navigation():
 im,d=canvas(2100,1370);box(d,(790,40,1310,210),'GREENFARM',42,True)
 nodes=[((40,410,680,650),'CỬA HÀNG\nCatalog → Chi tiết → Giỏ\nCheckout → Đơn → Hóa đơn'),((740,410,1380,650),'TÀI KHOẢN\nHồ sơ • Địa chỉ • Yêu thích\nĐiểm • Hỗ trợ • Thông báo'),((1440,410,2080,650),'PORTAL VẬN HÀNH\nĐăng nhập theo vai trò\nAdmin • Staff • Delivery')]
 for r,t in nodes:box(d,r,t,34,True);arrow(d,(1050,210),((r[0]+r[2])/2,410))
 box(d,(50,940,680,1260),'KHÁCH VÃNG LAI\nPhiên giỏ → Đặt COD/VNPAY\nTra cứu bằng thông tin xác minh\nHóa đơn đơn khách',34)
 arrow(d,(360,650),(360,940))
 box(d,(740,940,1380,1260),'ADMIN VÀ STAFF\nĐơn • Sản phẩm • Liên hệ\nKho • Coupon • Người dùng\nBáo cáo theo quyền',34)
 box(d,(1440,940,2080,1260),'GIAO HÀNG\nĐơn được phân công\nNhận → Đi giao → Kết quả',34)
 arrow(d,(1680,650),(1060,940));arrow(d,(1840,650),(1760,940));save(im,'navigation')

def wires(key,admin=False):
 im,d=canvas(2100,1340)
 if not admin:
  box(d,(40,40,2060,160),'Logo GreenFarm     Tìm sản phẩm     Tài khoản     Yêu thích     Giỏ',34)
  box(d,(40,210,470,1150),'BỘ LỌC\n\nDanh mục\nKhoảng giá\nCòn / hết hàng',36,True)
  for x in [520,1030,1540]:
   box(d,(x,210,x+460,610),'Ảnh sản phẩm\n\nTên • Giá • Đơn vị\n[ Thêm vào giỏ ]',32)
  box(d,(520,660,1310,1150),'CHECKOUT\n\nNgười nhận / Địa chỉ\nGiao nội bộ hoặc GHN\nCoupon hàng và phí giao\nĐiểm (tài khoản)\nCOD hoặc VNPAY',34,True)
  box(d,(1380,660,2060,1150),'TỔNG TIỀN TỪ BACKEND\nTạm tính − Giảm hàng − Điểm\n+ Phí giao − Giảm phí\n\nTổng thanh toán\n[ Xác nhận đặt hàng ]',32,True)
 else:
  box(d,(40,40,2060,160),'GreenFarm Admin       Thông báo       Tài khoản',36,True)
  box(d,(40,210,450,1240),'ĐIỀU HƯỚNG\n\nTổng quan\nĐơn hàng\nSản phẩm\nDanh mục\nKho / Nhà cung cấp\nCoupon\nNgười dùng\nLiên hệ\nGiao nhận',32,True)
  for x,t in [(510,'Tồn khả dụng'),(1030,'Lô gần hết hạn'),(1550,'Đơn cần xử lý')]:box(d,(x,210,x+480,380),t,32,True)
  box(d,(510,430,2030,580),'Tìm kiếm       Trạng thái       Hạn dùng       [ Nhập lô / Điều chỉnh ]',30)
  box(d,(510,630,2030,1040),'DANH SÁCH SẢN PHẨM VÀ LÔ\n\nMã / Tên     Còn vật lý     Đang giữ     Khả dụng     Hạn dùng\n\nChi tiết lô • Lịch sử • Người thực hiện • Lý do',33,True)
  box(d,(510,1090,2030,1240),'Tab: Tồn sản phẩm  |  Lô  |  Lịch sử  |  Nhà cung cấp      Phân trang',31)
 txt(d,(1050,1290),'Bố cục thiết kế minh họa; không phải ảnh chụp triển khai thực tế',28);save(im,key)

def main():
 context()
 usecase('use_overview', [('Khách','L',0),('Customer','L',1),('Staff','L',2),('Giao hàng','L',3),('Admin','L',4),('Dịch vụ ngoài','R',2)],
 [('Tra cứu và mua hàng',0,0),('Tài khoản và quyền lợi',0,1),('Xử lý đơn và hỗ trợ',0,2),('Nhận và giao đơn',0,3),('Quản lý và báo cáo',0,4),('Thanh toán và hoàn tiền',1,0),('Vận đơn GHN',1,1),('Email và hình ảnh',1,2),('Chatbot tư vấn',1,3),('Kiểm soát kho FEFO',1,4)],
 [(0,0),(1,1),(2,2),(3,3),(4,4),(5,5),(5,6),(5,7),(5,8)],'HỆ THỐNG GREENFARM')
 usecase('use_shopping',[('Khách vãng lai','L',1),('Khách hàng','R',2)],
 [('Xem và lọc sản phẩm',0,0),('Giỏ khách và đặt hàng',0,1),('Tra cứu đơn khách',0,2),('Liên hệ và tư vấn',0,3),('Đăng ký / Đăng nhập',0,4),('Hồ sơ và địa chỉ',1,0),('Giỏ và yêu thích',1,1),('Đặt và theo dõi đơn',1,2),('Hủy / Yêu cầu hoàn tiền',1,3),('Đánh giá và điểm',1,4)],
 [(0,0),(0,1),(0,2),(0,3),(0,4),(1,0),(1,3),(1,5),(1,6),(1,7),(1,8),(1,9)],'USE CASE MUA SẮM')
 usecase('use_operations',[('Quản trị viên','L',2),('Nhân viên','R',1),('Giao hàng','R',4)],
 [('Tài khoản và catalog',0,0),('Coupon và báo cáo',0,1),('Nhà cung cấp / Nhập kho',0,2),('Vận đơn GHN / Hoàn tiền',0,3),('Điều phối giao nhận',0,4),('Xử lý và phân công đơn',1,0),('Điều chỉnh kho',1,1),('Liên hệ và sản phẩm',1,2),('Nhận đơn được giao',1,3),('Cập nhật kết quả giao',1,4)],
 [(0,0),(0,1),(0,2),(0,3),(0,4),(1,5),(1,6),(1,7),(2,8),(2,9)],'USE CASE QUẢN TRỊ VÀ VẬN HÀNH')
 sequence('seq_auth',['Người dùng','Giao diện','Auth / Security','MySQL'],[
 (0,1,'Nhập tài khoản và cổng vai trò'),(1,2,'POST auth/login'),(2,3,'Đọc tài khoản và khóa tạm'),(3,2,'Mật khẩu băm và role',True),(2,2,'Kiểm tra BCrypt và role'),(2,3,'Lưu hash refresh token'),(2,1,'JWT, refresh và hồ sơ',True),(1,2,'POST auth/refresh khi cần'),(2,3,'Khóa, thu hồi và xoay refresh'),(2,1,'Cặp token mới',True)],'Sai mật khẩu/khóa/sai cổng → từ chối; dùng lại refresh đã thay thế → thu hồi phiên.')
 sequence('seq_checkout',['Khách hàng','Giao diện','OrderService','Kho / Ưu đãi','MySQL'],[
 (0,1,'Chọn địa chỉ, coupon và điểm'),(1,2,'POST checkout/preview'),(2,3,'Đọc giá, phí, tồn và ưu đãi'),(2,1,'Breakdown và tổng',True),(1,2,'POST orders'),(2,4,'Khóa user, sản phẩm và coupon'),(2,3,'Giữ lô FEFO; giữ lượt; dùng điểm'),(3,4,'Lưu phân bổ và lịch sử điểm'),(2,4,'Lưu đơn, dòng snapshot và payment'),(2,1,'Đơn và URL VNPAY nếu có',True)],'Các thay đổi dữ liệu tạo đơn nằm trong transaction; kiểm tra lại điều kiện trước commit.')
 sequence('seq_guest',['Khách','Giao diện','GuestCommerce','MySQL'],[
 (0,1,'Nhập liên hệ và địa chỉ'),(1,2,'Gửi token và idempotencyKey'),(2,3,'Khóa phiên; tìm checkout cùng khóa'),(2,3,'Nếu mới: tạo đơn và giữ tài nguyên'),(2,1,'Order, lookupToken và paymentUrl',True),(1,2,'Mất phản hồi: gọi recovery'),(2,3,'Tìm kết quả đã commit'),(2,1,'Trả lại đơn cũ',True),(1,2,'Tra cứu: ID, email, lookupToken'),(2,1,'Chi tiết sau xác minh',True)],'Không tạo tài khoản tự động; khóa checkout ngăn trừ kho hai lần khi client gửi lại.')
 sequence('seq_payment',['Người mua','Frontend','PaymentService','VNPAY','MySQL'],[
 (1,2,'Yêu cầu đặt đơn VNPAY'),(2,4,'Lưu payment pending và hạn'),(2,1,'URL đã ký',True),(1,3,'Chuyển đến thanh toán'),(3,2,'Return hoặc IPN có chữ ký'),(2,2,'Kiểm tra HMAC, mã và số tiền'),(2,4,'Khóa và cập nhật một lần'),(2,1,'Kết quả đã xác minh',True),(1,2,'Đọc chi tiết / hóa đơn')],'Callback sai không ghi thành công; payment hết hạn được scheduler hủy và trả tài nguyên.')
 sequence('seq_refund',['Khách hàng','RefundService','Admin','VNPAY','MySQL'],[
 (0,1,'Gửi yêu cầu và lý do'),(1,4,'Kiểm tra chủ đơn/payment; lưu pending'),(2,1,'Duyệt hoặc từ chối'),(1,4,'Khóa yêu cầu; lưu request refund'),(1,3,'Gửi refund có chữ ký'),(3,1,'Kết quả và chữ ký',True),(1,1,'Xác minh kết quả gateway'),(1,4,'Thành công đủ điều kiện: hoàn/hủy'),(1,0,'Trạng thái và thông báo',True)],'Kết quả processing chưa phải hoàn tất. Hoàn COD được xác nhận nội bộ; hoàn một phần không tự hủy toàn đơn.')
 sequence('seq_delivery',['Staff / Admin','OrderLifecycle','MySQL','Giao hàng'],[
 (0,1,'Xác nhận processing'),(1,2,'Xuất lô; lịch sử; payment kiểm tra'),(0,1,'Sẵn sàng và phân công nội bộ'),(1,2,'Lưu người được phân công'),(3,1,'Nhận đơn của mình'),(1,2,'Lưu mốc claim nếu chưa có'),(3,1,'Bắt đầu giao'),(3,1,'Giao thành công hoặc thất bại'),(1,2,'Lưu trạng thái, COD, điểm, lịch sử')],'Giao thất bại cần lý do; chỉ người được phân công thao tác; đơn GHN dùng luồng riêng.')
 sequence('seq_ghn',['Admin','GhnService','GHN staging','MySQL'],[
 (0,1,'Tạo vận đơn đơn sẵn sàng'),(1,2,'Tra GF-orderId; kiểm tra lại phí'),(2,1,'Phí và kết quả tra cứu',True),(1,2,'Tạo nếu chưa có vận đơn'),(1,3,'Lưu mã và hồ sơ vận chuyển'),(2,1,'Webhook trạng thái hoặc COD'),(1,1,'Kiểm tra secret/shop/mã/thời gian'),(1,3,'Lưu event nếu chưa có'),(1,3,'Cập nhật trạng thái và đối soát COD'),(1,2,'204 nếu tiếp nhận hợp lệ',True)],'Timeout tạo cần tra lại; delivered không tự xác nhận COD nếu GHN chưa chuyển tiền khớp.')
 sequence('seq_inventory',['Đặt / Xử lý đơn','InventoryService','MySQL','Hủy đơn'],[
 (0,1,'Giữ hàng khi tạo đơn'),(1,2,'Khóa sản phẩm và lô còn hạn'),(1,2,'Chọn FEFO; tăng reserved; lưu allocation'),(0,1,'Chuyển processing'),(1,2,'Kiểm tra hạn; phân bổ lại nếu cần'),(1,2,'Giảm remaining/reserved; EXPORT'),(3,1,'Hủy hợp lệ'),(1,2,'Giải phóng hoặc hoàn đúng allocation'),(1,2,'Ghi lịch sử và đồng bộ stock')],'Mỗi phân bổ chỉ được hoàn một lần; không điều chỉnh kho xuống thấp hơn phần đang giữ.')
 sequence('seq_chat',['Người dùng','Giao diện','ChatbotService','MySQL / Catalog','Gemini'],[
 (0,1,'Gửi câu hỏi'),(1,2,'message và phiên chat'),(2,2,'Che dữ liệu; kiểm tra phạm vi'),(2,3,'Đọc lịch sử và catalog công khai'),(3,2,'Ngữ cảnh đã giới hạn',True),(2,4,'Prompt backend'),(4,2,'Phản hồi hoặc lỗi',True),(2,2,'Lọc, đối chiếu hoặc fallback'),(2,3,'Lưu hội thoại đã lọc'),(2,1,'Tư vấn và thẻ sản phẩm',True)],'Không đọc hồ sơ/đơn riêng; không tạo đơn, định giá hoặc xác nhận thanh toán qua AI.')
 states();architecture();deployment();ai();concept();ers();navigation();wires('wire_store');wires('wire_admin',True)
 print('Figures',len(list(OUT.glob('*.png'))))
if __name__=='__main__':main()
