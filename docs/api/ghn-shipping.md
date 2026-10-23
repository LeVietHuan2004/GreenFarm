# Tích hợp Giao Hàng Nhanh (GHN) — staging

## Trạng thái và phạm vi

GHN là nhà vận chuyển tùy chọn, độc lập với giao hàng nội bộ. Tính năng tắt mặc định (`GHN_ENABLED=false`). Backend chỉ cho phép gateway staging `https://dev-online-gateway.ghn.vn`; cấu hình URL production sẽ bị từ chối. Frontend chỉ gọi API GreenFarm, không gọi GHN hoặc giữ token GHN.

Sản phẩm cũ có `ghn_eligible=false`. Quản trị viên chỉ bật GHN khi đã xác minh sản phẩm thực sự được GHN nhận vận chuyển, đồng thời khai báo khối lượng (g) và kích thước đóng gói (cm). Giỏ có bất kỳ sản phẩm chưa đủ điều kiện sẽ không thể chọn GHN. Không tự chia đơn hay tự bật cho rau, quả, thịt, cá tươi hoặc hàng đông lạnh.

## Cấu hình tài khoản staging

1. Đăng nhập **GHN Developer staging** tại `https://developer.ghn.dev`. Tạo shop hoặc chọn shop staging. Lấy **Token API staging** trong mục quản lý token và **Shop ID staging** trong mục cửa hàng / Lấy Shop. Token và shop phải thuộc cùng tài khoản, cùng môi trường.
2. Thêm các biến dưới đây vào `D:\GreenFarm\.env` trên máy chạy Docker Compose. Không gửi token hoặc secret trong chat và không commit `.env`.

   ```dotenv
   GHN_ENABLED=true
   GHN_BASE_URL=https://dev-online-gateway.ghn.vn
   GHN_TOKEN=<token staging của bạn>
   GHN_SHOP_ID=<shop id staging của bạn>
   GHN_WEBHOOK_SECRET=<chuỗi bí mật ngẫu nhiên dài, do bạn tạo>
   GHN_SHIPPING_PAYMENT_TYPE=1
   GHN_REQUIRED_NOTE=CHOXEMHANGKHONGTHU
   ```

3. Khi có URL backend HTTPS công khai, vào **GHN Developer staging → tài khoản → Cấu hình webhook → Đơn hàng**. Điền URL `https://<backend-domain>/api/webhooks/ghn/orders`. Thêm custom header `X-GreenFarm-GHN-Secret` với đúng giá trị `GHN_WEBHOOK_SECRET`. Bật sự kiện `switch_status` và `cod` (có thể giữ các sự kiện khác để lưu lịch sử). Lưu cấu hình. GHN có thể mất khoảng 15 phút để áp dụng thay đổi webhook.

Shop staging phải có tên, số điện thoại và địa chỉ lấy hàng đúng nơi GreenFarm gửi hàng. Backend hiện dùng địa chỉ mặc định của shop trong GHN; không có biến `.env` riêng cho địa chỉ lấy hàng. Nếu đổi sang shop staging khác, cập nhật `GHN_SHOP_ID` rồi tạo lại container backend. Lần kiểm tra local ngày 20/10/2026 cho thấy Shop ID thuộc token và đang hoạt động. Sau khi nhập địa chỉ, `shop/all` đã có `address` và `address_v2`, nhưng mã tỉnh/phường mới tương ứng Hà Nội / Phường Dương Nội trong khi GHN báo không tìm thấy tỉnh Lạng Sơn khi xem trước phí. Cần kiểm tra lại toàn bộ địa chỉ lấy hàng và địa chỉ trả hàng trên GHN staging để tên địa chỉ khớp các lựa chọn tỉnh/phường, rồi mới thử báo phí hoặc tạo vận đơn.

Webhook cần URL công khai; `localhost:8080` không nhận được callback từ GHN. Không dùng token GHN làm webhook secret. Endpoint trả `204` cho sự kiện hợp lệ hoặc lặp; xác thực secret, ShopID, mã vận đơn và `ClientOrderCode`, lưu sự kiện theo `OrderCode + Type + Time`, không cho trạng thái cũ ghi đè trạng thái mới.

## Cập nhật ứng dụng local

Migration V24 thêm các cột địa chỉ, thông số đóng gói, nhà vận chuyển và hai bảng `shipments`, `shipment_events`. Flyway chạy migration khi backend mới khởi động. Trước khi áp dụng lên database thật, sao lưu MySQL hiện có và kiểm tra `flyway_schema_history` đang ở V23. Không xóa volume/database.

Sau khi đã cấu hình `.env` và sao lưu, chạy tại `D:\GreenFarm`:

```powershell
docker compose up -d --build --no-deps backend
docker compose ps
docker compose logs --tail=150 backend
```

`docker compose restart backend` chỉ đọc lại biến môi trường vốn đã được gán cho container hiện tại; nếu sửa `.env` trong Compose, phải **recreate** container. Nếu sửa Java/migration, phải **rebuild image và recreate** bằng lệnh trên. Frontend chạy `npm run dev` đọc mã mới khi reload; bản frontend Docker/production cũng cần build lại riêng.

Kiểm tra API trạng thái `GET http://localhost:8080/api/public/ghn/status` phải trả `available: true`, rồi gọi `/api/public/ghn/provinces` và `/api/public/ghn/wards?provinceId=<id>` để xác nhận token/ShopId hoạt động. Không in giá trị token trong log hoặc ảnh chụp màn hình.

## Luồng nghiệp vụ

1. Admin đánh dấu từng sản phẩm đủ điều kiện và nhập thông số gói hàng.
2. Khách chọn địa chỉ tỉnh/thành và phường/xã theo danh mục 2 cấp GHN; địa chỉ đường/số nhà nhập riêng. Checkout gọi `shipping-order/preview`. Backend kiểm tra điều kiện hàng, tuyến, phí và tổng tiền lần nữa lúc tạo đơn. Nếu GHN không báo phí hợp lệ, khách chọn giao nội bộ.
3. Đơn GHN ban đầu chỉ có hồ sơ vận chuyển nội bộ, chưa tạo vận đơn tại GHN. Nhân viên xử lý đơn đến `ready_for_delivery`; admin mở chi tiết đơn và nhấn **Tạo vận đơn GHN**. Đơn VNPAY phải được xác nhận thanh toán trước. Backend kiểm tra lại phí; dùng `GF-<orderId>` làm `client_order_code`, tra cứu theo mã này trước khi tạo và sau timeout.
4. Admin xem mã vận đơn, phí, tiền COD, dự kiến giao, lịch sử, đồng bộ thủ công, in phiếu và hủy vận đơn. Chỉ admin được gọi các API tạo/hủy. Nếu vận đơn đã tồn tại, phải hủy GHN thành công trước khi hủy đơn GreenFarm.
5. Khách xem trạng thái và mã vận đơn trong chi tiết đơn đã xác thực; khách vãng lai phải tra cứu bằng email + mã xác minh đơn. Webhook `delivered` cập nhật trạng thái giao nhưng không tự hoàn tất thanh toán COD. COD chỉ hoàn tất khi có thời điểm GHN chuyển tiền và số tiền khớp; VNPAY vẫn theo callback của VNPAY.

## API GreenFarm

| Vai trò | Endpoint | Công dụng |
| --- | --- | --- |
| Công khai | `GET /api/public/ghn/status` | Kiểm tra GHN đã bật và đủ cấu hình |
| Công khai | `GET /api/public/ghn/provinces` | Danh sách tỉnh/thành 2 cấp |
| Công khai | `GET /api/public/ghn/wards?provinceId=...` | Danh sách phường/xã |
| Khách | `POST /api/checkout/preview`, `POST /api/orders` | `shippingProvider: "ghn"`, `expectedTotal` khi tạo đơn |
| Khách vãng lai | `POST /api/public/guest/checkout/preview`, `POST /api/public/guest/orders` | Cùng dữ liệu provider, thêm `ghnProvinceId`, `ghnWardId` |
| Admin | `POST /api/admin/orders/{id}/ghn/create` | Tạo hoặc khôi phục vận đơn |
| Admin | `POST /api/admin/orders/{id}/ghn/sync` | Tra cứu GHN và đồng bộ trạng thái |
| Admin | `POST /api/admin/orders/{id}/ghn/cancel` | Hủy nếu GHN chấp nhận |
| Admin | `GET /api/admin/orders/{id}/ghn/print` | Lấy URL phiếu gửi tạm thời |
| Admin | `GET /api/admin/orders/{id}/ghn/events` | Lịch sử webhook/đồng bộ |
| GHN | `POST /api/webhooks/ghn/orders` | Webhook có custom secret header |

## Giới hạn cần nhớ

- Mô hình đóng gói hiện dùng một kiện ước tính: cộng khối lượng và chiều cao các món, lấy chiều dài/rộng lớn nhất; giới hạn 50 kg và 200 cm mỗi chiều. Admin cần kiểm tra kiện thực tế trước khi tạo vận đơn. Chưa hỗ trợ chia nhiều kiện hoặc đổi COD sau khi đặt.
- GHN dùng VND nguyên và COD tối đa 50 triệu VND. Đơn có tổng tiền lẻ hoặc vượt giới hạn sẽ không được chọn GHN.
- Phí GHN thực tế lúc tạo có thể khác phí checkout; hệ thống chặn tạo nếu phí preview đã đổi. Admin xử lý thủ công thay vì tự thu thêm tiền khách.
- Ngày 20/10/2026, backend local đã được build lại với GHN staging; Flyway V24 áp dụng thành công sau khi sao lưu MySQL. `/api/public/ghn/status` trả `available=true`; API tỉnh/thành trả 34 mục và API phường/xã trả dữ liệu. `shop/all` xác nhận Shop ID thuộc token và đang hoạt động. Xem trước phí GHN staging hiện trả `USER_ERR_COMMON` do không tìm thấy tỉnh của địa chỉ lấy hàng; chưa có báo phí thành công hoặc vận đơn thử nghiệm. Webhook không có secret trả `401`; chưa kiểm thử webhook từ GHN vì backend local chưa có URL HTTPS công khai. Unit test dùng mock; test tích hợp MySQL qua Testcontainers đã được chạy ngoài Docker build.

Tài liệu tham khảo: [GHN tạo đơn](https://developer.ghn.vn/vi/docs/order/create), [xem trước đơn](https://developer.ghn.vn/vi/docs/order/preview), [địa chỉ 2 cấp](https://developer.ghn.vn/vi/docs/master-data/get-province-new), [webhook](https://developer.ghn.vn/en/docs/webhook/callback-order-status).
