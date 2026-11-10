from pathlib import Path
import json,re
from usecases import UCS,FIG_AFTER
P=Path(__file__).parent
def table(title,headers,rows):
 return '@TABLE '+title+'\n| '+' | '.join(headers)+' |\n| '+' | '.join(['---']*len(headers))+' |\n'+'\n'.join('| '+' | '.join(str(c).replace('\n','<br>') for c in row)+' |' for row in rows)+'\n'
index=table('Danh sách use case và yêu cầu liên quan',['Mã','Tên use case','Yêu cầu'],[(f'UC-{i:02}',u[0],u[2]) for i,u in enumerate(UCS,1)])
detail=[]
for i,(name,actor,fr,priority,pre,post,flow,alt,exc) in enumerate(UCS,1):
 rows=[('Mã và tên',f'UC-{i:02} — {name}'),('Tác nhân và ưu tiên',actor+'; ưu tiên '+priority.lower()+'. '+fr+'.'),('Kích hoạt và tiền điều kiện',pre),('Điều kiện sau',post),('Luồng chính',flow),('Luồng thay thế',alt),('Ngoại lệ',exc)]
 detail.append(f'#### 3.3.4.{i} UC-{i:02} {name}\n\n'+table(f'Đặc tả use case UC-{i:02}',['Thuộc tính','Nội dung'],rows))
 if i in FIG_AFTER:
  fid,cap=FIG_AFTER[i];detail.append(f'@FIG {fid} {cap}\n')
 if i==12:
  detail.append('Các trạng thái dưới đây là trạng thái đơn GreenFarm; trạng thái vận đơn GHN được ánh xạ riêng. Nhánh hoàn tiền là nghiệp vụ có điều kiện, không phải đường hủy tự do từ mọi trạng thái.\n\n@FIG order_states Sơ đồ trạng thái vòng đời đơn hàng\n')

DICT=[
('Từ điển dữ liệu tài khoản và phiên',[
 ('users','id; role_id; email; password','BIGINT; VARCHAR','PK; FK roles; email unique; password BCrypt. Có status, credential_version, bộ đếm khóa và loyalty_points_balance.'),
 ('roles; permissions','id; name','BIGINT; VARCHAR','PK; tên role/permission duy nhất; role_permissions(role_id, permission_id) là bảng nối N–N.'),
 ('refresh_tokens','id; user_id; token_hash; expires_at; revoked_at; replaced_by_hash','BIGINT; CHAR(64); TIMESTAMP','Lưu hash duy nhất, hạn và quan hệ xoay vòng; không lưu refresh token thô.'),
 ('greenfarm_password_reset_tokens','id; user_id; token_hash; credential_version; expires_at; used_at','BIGINT; CHAR(64); INT; DATETIME','Token reset hiện tại; hash unique; kiểm tra hạn, phiên bản và thời điểm đã dùng.'),
 ('password_reset_tokens','email; token; created_at','VARCHAR; TIMESTAMP','Bảng kế thừa từ baseline; email là khóa chính, không dùng cho luồng reset hiện tại.'),
 ('shipping_addresses','id; user_id; full_name; phone; address; city; default','BIGINT; VARCHAR; BOOLEAN','Địa chỉ theo user; cột default xác định mặc định; có ghn_province_id/name và ghn_ward_id/name cho GHN.')]),
('Từ điển dữ liệu catalog giỏ và phiên khách',[
 ('categories','id; name; slug; description; image','BIGINT; VARCHAR; TEXT','Tên/slug unique; ảnh URL. Có trường tên/mô tả tiếng Anh trong schema.'),
 ('products','id; category_id; slug; price; stock; status; unit','BIGINT; VARCHAR; DECIMAL; INT; ENUM','FK danh mục; stock là tồn khả dụng đồng bộ; có origin, certification, storage_instructions.'),
 ('products','ghn_eligible; package_weight_g; package_length_cm/width_cm/height_cm','BOOLEAN; INT','Điều kiện GHN và gói hàng; thông số không tự chứng minh hàng được nhận vận chuyển.'),
 ('product_images','id; product_id; image','BIGINT; VARCHAR','Ảnh 1–N theo sản phẩm; lưu đường dẫn, không lưu nhị phân ảnh.'),
 ('cart_items; wishlists','id; user_id; product_id; quantity (giỏ)','BIGINT; INT','Unique user–product từng bảng; chỉ giỏ có quantity dương.'),
 ('guest_sessions','id; token_hash; expires_at; consumed_at; merged_user_id','BIGINT; CHAR(64); TIMESTAMP','Hash unique, thời hạn, mốc tiêu thụ và tài khoản gộp; không tự tạo user.'),
 ('guest_cart_items','id; guest_session_id; product_id; quantity','BIGINT; INT','FK phiên/sản phẩm; unique cặp phiên–sản phẩm; quantity dương.')]),
('Từ điển dữ liệu đơn hàng và thanh toán',[
 ('orders','id; user_id; guest_session_id; delivery_staff_id; shipping_address_id','BIGINT','PK và các FK; user/địa chỉ tài khoản có thể rỗng với đơn khách.'),
 ('orders','subtotal; discount_amount; loyalty_discount_amount; shipping_fee; shipping_discount_amount; total_price','DECIMAL','Các thành phần tiền được tính tại backend; có loyalty_points_used và snapshot mã coupon.'),
 ('orders','status; recipient_name/phone; shipping_address_line/city; shipping_provider','ENUM; VARCHAR','Trạng thái và snapshot giao nhận; có tỉnh/phường GHN, mốc nhận/giao, lý do thất bại.'),
 ('orders','guest_lookup_token_hash; guest_checkout_key','CHAR(64); VARCHAR(64)','Xác minh tra cứu và chống đặt lặp; khóa checkout gắn ngữ cảnh guest session.'),
 ('order_items','id; order_id; product_id; product_name/unit/image; quantity; price','BIGINT; VARCHAR; INT; DECIMAL','FK đơn/sản phẩm; snapshot giá/tên/ảnh/đơn vị và thông số gói hàng.'),
 ('order_status_history','id; order_id; status; changed_at; note','BIGINT; ENUM; TIMESTAMP; TEXT','Dòng lịch sử theo đơn, trạng thái và thời điểm; note ghi ngữ cảnh thao tác.'),
 ('payments','id; order_id; payment_method; amount; status; reference_code; transaction_id','BIGINT; ENUM; DECIMAL; VARCHAR','Mã reference_code unique; order_id có index nhưng chưa unique; nghiệp vụ hiện dùng một payment cho đơn.'),
 ('payments','invoice_email_sent_at; invoice_email_attempts; invoice_email_next_retry_at; invoice_email_last_error','TIMESTAMP; INT; VARCHAR(500)','Theo dõi gửi hóa đơn và retry; gateway_payload lưu dữ liệu phục vụ đối chiếu.'),
 ('refund_requests','id; order_id; reason; details; status; reviewed_by; reviewed_at; admin_note','BIGINT; VARCHAR; DATETIME','Lý do khách, trạng thái và quyết định admin; không thay thế bản ghi gateway.'),
 ('vnpay_refunds','id; payment_id; request_id; refund_amount; transaction_type; status','BIGINT; VARCHAR; DECIMAL','request_id unique; lưu mã phản hồi/trạng thái giao dịch và response_payload.')]),
('Từ điển dữ liệu coupon và điểm thưởng',[
 ('coupons','id; code; coupon_type; discount_type; scope_type','BIGINT; VARCHAR','Mã unique; loại giảm hàng/miễn giao, cách giảm và phạm vi ALL/CATEGORY/PRODUCT.'),
 ('coupons','discount_percentage; discount_amount; max_discount_amount; minimum_order_amount','INT; DECIMAL','Giá trị, trần giảm và tối thiểu; có starts_at, expires_at, is_active và giới hạn lượt.'),
 ('coupon_categories; coupon_products','coupon_id; category_id hoặc product_id','BIGINT','PK ghép và FK; định nghĩa phạm vi áp dụng coupon.'),
 ('coupon_usages','id; coupon_id; order_id; user_id; guest_session_id; discount_amount; status','BIGINT; DECIMAL; VARCHAR','Unique coupon–order; chủ tài khoản hoặc khách; RESERVED/USED/RELEASED và các mốc thời gian.'),
 ('loyalty_point_transactions','id; user_id; order_id; review_id; product_id; type; points; description','BIGINT; VARCHAR; INT','Điểm có dấu; truy vết thưởng, đổi, hoàn/điều chỉnh; khóa nghiệp vụ chống xử lý lặp.')]),
('Từ điển dữ liệu kho và nguồn cung',[
 ('suppliers','id; supplier_code; name; phone; email; address; status; note','BIGINT; VARCHAR','supplier_code unique; thông tin nguồn cung, liên hệ và trạng thái sử dụng.'),
 ('inventory_batches','id; batch_code; product_id; supplier_id; quantity; remaining_quantity; reserved_quantity','BIGINT; VARCHAR(64); INT','Mã lô unique; số nhập, còn vật lý và đang giữ; tham chiếu sản phẩm/nhà cung cấp.'),
 ('inventory_batches','import_price; manufacture_date; expiry_date; imported_at; created_by','DECIMAL; DATE; DATETIME; BIGINT','Giá nhập, ngày sản xuất/hạn, thời điểm và người nhập; lô mới yêu cầu hạn dùng.'),
 ('order_inventory_allocations','id; order_item_id; batch_id; quantity; status; exported_at; restored_at','BIGINT; INT; VARCHAR; DATETIME','Nối dòng đơn với lô; trạng thái giữ/xuất/hoàn và mốc chống hoàn lặp.'),
 ('inventory_transactions','id; product_id; batch_id; type; quantity; quantity_before/after; reserved_before/after','BIGINT; VARCHAR; INT','Nhật ký biến động lô và phần giữ; có created_by, reference_id, reason và created_at.')]),
('Từ điển dữ liệu giao nhận và tương tác',[
 ('shipments','id; order_id; provider; provider_order_code; client_order_code; shop_id','BIGINT; VARCHAR; INT','Unique order/mã khách/mã GHN; liên kết một hồ sơ vận chuyển theo đơn.'),
 ('shipments','customer_shipping_fee; carrier_shipping_fee; cod_amount; shipping_status; last_event_at; cod_transferred_at','DECIMAL; VARCHAR; DATETIME','Phí khách/phí hãng, COD, trạng thái và mốc sự kiện/chuyển tiền; có last_error.'),
 ('shipment_events','id; shipment_id; event_key; event_type; shipping_status; event_at','BIGINT; VARCHAR; DATETIME','Unique shipment–event_key; lịch sử webhook/sync và mô tả.'),
 ('reviews','id; user_id; product_id; rating; comment','BIGINT; INT; TEXT','Unique user–product; rating 1–5; được service kiểm tra đã mua/nhận.'),
 ('contacts','id; user_id; full_name; email; phone_number; message; status; response; responded_by/at','BIGINT; VARCHAR; TEXT; TIMESTAMP','Khách có thể không có user_id; dữ liệu phản hồi và người xử lý.'),
 ('notifications','id; user_id; type; message; link; is_read','BIGINT; VARCHAR; TEXT; BOOLEAN','Thông báo riêng theo user; link dẫn tới tài nguyên liên quan.'),
 ('chat_messages','id; user_id; guest_token_hash; sender; message; created_at','BIGINT; CHAR(64); ENUM; TEXT; TIMESTAMP','Tách lịch sử user/guest hash; nội dung được lọc trước lưu/gửi AI.')])]
dictionary='\n'.join(table(title,['Bảng','Trường trọng tâm','Kiểu chính','Ý nghĩa và ràng buộc'],rows) for title,rows in DICT)

api=json.loads((P/'api_inventory.json').read_text(encoding='utf8'))
labels={
'AuthController':('Xác thực và phiên','UC-01–03'), 'UserController':('Hồ sơ và mật khẩu','UC-03'),
'CategoryPublicController':('Danh mục công khai','UC-04'),'ProductPublicController':('Sản phẩm công khai','UC-04'),
'ReviewPublicController':('Đánh giá công khai','UC-04,18'),'CouponPublicController':('Gợi ý coupon','UC-17'),
'ChatPublicController':('Hội thoại tư vấn','UC-20'),'ContactPublicController':('Gửi liên hệ','UC-19'),
'GuestCommercePublicController':('Phiên giỏ và đơn khách','UC-05,08'),'GhnPublicController':('Trạng thái và địa chỉ GHN','UC-07,13'),
'HealthController':('Health backend','Vận hành'),'CartController':('Giỏ và gộp giỏ','UC-05,06'),
'WishlistController':('Yêu thích','UC-05'),'ShippingAddressController':('Địa chỉ','UC-07'),
'CheckoutController':('Preview checkout','UC-07'),'OrderController':('Đơn và hậu mãi khách','UC-07,09,10'),
'PaymentController':('Thanh toán và callback','UC-09'),'ReviewController':('Đánh giá đã mua','UC-18'),
'LoyaltyController':('Điểm thưởng','UC-18'),'NotificationController':('Thông báo và SSE','UC-19'),
'ContactController':('Liên hệ của tài khoản','UC-19'),'UserAdminController':('Quản lý người dùng','UC-21'),
'RoleAdminController':('Danh sách role/quyền','UC-21'),'CategoryAdminController':('Quản lý danh mục','UC-14'),
'ProductAdminController':('Quản lý sản phẩm/ảnh','UC-14'),'UploadAdminController':('Upload ảnh dùng chung','UC-14'),
'CouponAdminController':('Quản lý coupon','UC-17'),'InventoryAdminController':('Kho admin','UC-15,16'),
'SupplierAdminController':('Nhà cung cấp','UC-15'),'OrderAdminController':('Đơn và hoàn tiền admin','UC-10–12'),
'RefundRequestAdminController':('Xét yêu cầu hoàn tiền','UC-10'),'GhnShipmentAdminController':('Vận đơn GHN','UC-13'),
'ShipmentAdminController':('Danh sách giao nhận','UC-13'),'BusinessReportAdminController':('Báo cáo/CSV','UC-22'),
'StaffOrderController':('Xử lý/phân công đơn','UC-11,12'),'StaffInventoryController':('Kho staff','UC-16'),
'ContactOperationsController':('Liên hệ vận hành','UC-19'),'DeliveryOrderController':('Giao hàng nội bộ','UC-12'),
'GhnWebhookController':('Webhook GHN','UC-13')}
api_tables=[]
groups=[('API công khai và xác thực',lambda x:'/publicapi/' in x['file'].replace('\\','/') or '/auth/' in x['file'].replace('\\','/')),
('API tài khoản và mua sắm',lambda x:'/customer/' in x['file'].replace('\\','/')),
('API quản trị',lambda x:'/admin/' in x['file'].replace('\\','/')),
('API vận hành và giao hàng',lambda x:any('/'+k+'/' in x['file'].replace('\\','/') for k in ['staff','delivery']))]
for title,predicate in groups:
 rows=[]
 for a in api:
  if not predicate(a):continue
  key=Path(a['file'].replace('\\','/')).stem;name,uc=labels[key]
  paths=[e['path'] for e in a['endpoints']]
  # The shared prefix is displayed explicitly; all suffixes remain exact.
  common=__import__('posixpath').commonpath(paths) if len(paths)>1 else paths[0]
  if common not in paths and '{' in common: common=common.rsplit('/',1)[0]
  entries={}
  for e in a['endpoints']:
   suf=e['path'][len(common):] or '/'
   entries.setdefault(suf,[]).append(e['method'])
  txt=common+'<br>'+'; '.join('/'.join(v)+' '+k for k,v in entries.items())
  rows.append((name,txt,uc))
 api_tables.append(table(title,['Nhóm chức năng','Đường dẫn gốc và phương thức theo hậu tố','Use case'],rows))
api_catalog='Các hậu tố trong cột API được nối vào đường dẫn gốc ở đầu ô; dấu / đứng riêng nghĩa là chính đường dẫn gốc, không thêm một cấp tài nguyên.\n\n'+'\n'.join(api_tables)

SCREENS=[
('Cửa hàng','/, /store, /products, /categories/[slug], /products/[slug]','UC-04','Catalog công khai, danh mục, ảnh và đánh giá'),
('Xác thực khách','/login, /register, /forgot-password, /reset-password','UC-01–03','Đăng ký/đăng nhập/khôi phục; auth API'),
('Hồ sơ cá nhân','/profile','UC-03','Hồ sơ, avatar, mật khẩu'),
('Giỏ và yêu thích','/cart, /wishlist','UC-05,06','Giỏ guest hoặc tài khoản; gộp và yêu thích'),
('Địa chỉ và checkout','/addresses, /checkout','UC-07,08,17,18','Địa chỉ, preview, coupon/điểm, provider và payment'),
('Đơn tài khoản','/orders, /orders/[id], /orders/[id]/invoice','UC-09,10,18','Chi tiết, lịch sử, hủy/hoàn, đánh giá và in hóa đơn'),
('Đơn khách và kết quả trả tiền','/guest-orders, /guest-orders/[id]/invoice, /payment-result','UC-08,09','Xác minh tra cứu, hóa đơn khách và kết quả backend'),
('Điểm và hỗ trợ','/points, /contact','UC-18,19','Số dư/lịch sử điểm, gửi và xem liên hệ'),
('Cổng admin','/admin/login, /admin, /admin/profile','UC-02,03,22','Đăng nhập, dashboard/báo cáo và hồ sơ'),
('Catalog và tài khoản admin','/admin/categories, /admin/products, /admin/users','UC-14,21','Quản lý danh mục/sản phẩm/ảnh và người dùng'),
('Đơn và giao nhận admin','/admin/orders, /admin/orders/[id], /admin/delivery','UC-10–13','Vận hành đơn, hoàn tiền, phân công và vận đơn GHN'),
('Kho ưu đãi hỗ trợ admin','/admin/inventory, /admin/coupons, /admin/contacts','UC-15–17,19','Lô/nhà cung cấp, coupon và liên hệ'),
('Portal staff','/staff/login, /staff, /staff/inventory, /staff/contacts','UC-02,11,12,16,19','Xử lý đơn, phân công, kiểm kê và hỗ trợ'),
('Portal giao hàng','/delivery/login, /delivery','UC-02,12','Đơn được giao, nhận và cập nhật kết quả')]
screens=table('Danh sách màn hình và chức năng tương ứng',['Nhóm màn hình','Tuyến giao diện','Use case','Dữ liệu và thao tác'],SCREENS)
screens+='\nKhung chatbot và menu thông báo có mặt trên các layout phù hợp, tương ứng UC-20 và UC-19; báo cáo nằm ở dashboard admin, quản lý nhà cung cấp nằm trong khu vực kho, không tạo thêm tuyến trang không có trong mã nguồn.\n'
md=(P/'chapter_main.md').read_text(encoding='utf8')
for key,val in [('@UC_INDEX',index),('@UC_DETAILS','\n'.join(detail)),('@DATA_DICTIONARY',dictionary),('@API_CATALOG',api_catalog),('@SCREEN_CATALOG',screens)]:md=md.replace(key,val)
counter={'TABLE':0,'FIG':0};figures=[]
def number(m):
 kind=m[1];counter[kind]+=1
 if kind=='TABLE':return f'Bảng 3.{counter[kind]}. '+m[2]
 fid,caption=m[2].split(' ',1);figures.append((fid,f'Hình 3.{counter[kind]}. '+caption))
 return f'![{figures[-1][1]}](figures_chuong3/{fid}.png)\n\n'+figures[-1][1]
md=re.sub(r'^@(TABLE|FIG) (.+)$',number,md,flags=re.M)
out=Path('output/report');out.mkdir(exist_ok=True,parents=True)
(P/'chapter_final.md').write_text(md,encoding='utf8')
(P/'figures.json').write_text(json.dumps(figures,ensure_ascii=False,indent=2),encoding='utf8')
print(counter,'Words',len(md.split()))
# Coverage: every actual page route and every declared table name should appear.
routes=['/'+str(f.parent.relative_to(Path('frontend/src/app'))).replace('\\','/').replace('.','') for f in Path('frontend/src/app').rglob('page.tsx')]
missing=[r for r in routes if r not in md]
print('Routes',len(routes),'missing',missing)
