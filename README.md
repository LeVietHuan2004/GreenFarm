# GreenFarm

GreenFarm là nền tảng kinh doanh nông sản trực tuyến được xây dựng lại từ đầu,
dựa trên SRS và schema dữ liệu của dự án cũ.

## Trạng thái hiện tại

Giai đoạn 1 đã triển khai module tài khoản:

- Đăng ký và đăng nhập bằng JWT.
- Khóa tài khoản 15 phút sau 5 lần đăng nhập sai liên tiếp.
- Phân quyền theo role và permission có sẵn trong database.
- Cổng đăng nhập riêng cho khách hàng, giao hàng và quản trị viên; backend từ chối
  tài khoản đăng nhập sai cổng.
- Xem, cập nhật hồ sơ và đổi mật khẩu.
- API quản trị để tìm kiếm, đổi role và trạng thái người dùng.
- Giao diện đăng nhập, đăng ký và hồ sơ responsive.
- Flyway quản lý thay đổi schema từ baseline dữ liệu cũ.

Giai đoạn 2 đã triển khai catalog sản phẩm:

- Module danh mục, sản phẩm và danh sách ảnh trên schema dữ liệu cũ.
- Catalog công khai có tìm kiếm, phân trang và lọc theo danh mục, khoảng giá,
  trạng thái còn hàng/hết hàng.
- Chi tiết sản phẩm theo slug; sản phẩm `hidden` không xuất hiện ở API public.
- API quản trị để thêm/sửa/ẩn sản phẩm, thêm/xóa ảnh bằng URL và CRUD danh mục.
- UI trang chủ cửa hàng, catalog, danh mục và chi tiết responsive.
- Khu vực quản trị có sidebar dùng chung, dashboard số liệu thật và màn quản lý
  người dùng, danh mục, sản phẩm.

Giai đoạn 3: giỏ hàng và yêu thích đã có API và UI `/cart`, `/wishlist`.
Khách hàng đăng nhập để thêm/xóa/cập nhật số lượng, lưu sản phẩm yêu thích;
giỏ hàng được kiểm tra tồn kho và tự tải lại theo tài khoản sau đăng nhập.
Giai đoạn 4 đã triển khai địa chỉ giao hàng, checkout và đơn hàng. Checkout tính
tạm tính, phí giao hàng, coupon và tổng tiền trên backend; tạo đơn kiểm tra/trừ
tồn kho trong transaction và lưu lịch sử trạng thái. UI gồm `/addresses`,
`/checkout`, `/orders` và `/orders/{id}`.

Giai đoạn 5 đã triển khai coupon và thanh toán:

- Coupon được kiểm tra trạng thái active, thời gian hiệu lực, usage limit và loại
  giảm giá trên backend trong lúc preview/tạo đơn.
- Checkout chọn COD hoặc VNPAY. COD tạo payment chờ thanh toán; VNPAY sandbox tạo
  URL ký HMAC-SHA512 và cập nhật kết quả qua Return/IPN đã xác thực.
- UI có lựa chọn phương thức, trang `/payment-result` và trạng thái payment trong
  chi tiết đơn. VNPAY chỉ được bật khi merchant credentials được cấu hình.
- Sau khi đặt COD hoặc VNPAY thành công, khách xem ngay hóa đơn tại
  `/orders/{id}/invoice`, có thể in/lưu PDF; hóa đơn HTML cũng được gửi vào email
  đăng ký khi SMTP được bật.

Giai đoạn 6 đã hoàn thiện quy trình vận hành đơn:

- Admin và staff xem đơn mới; staff xác nhận, từ chối và chuyển đơn sang trạng thái
  sẵn sàng giao.
- Admin phân công tài khoản `delivery_staff` đang hoạt động cho đơn sẵn sàng giao.
- Delivery staff phải nhận đơn trước khi bắt đầu giao, sau đó cập nhật giao thành công
  hoặc thất bại. Đơn thất bại có thể được staff đưa lại hàng chờ giao và phân công lại.
- Mọi lần đổi trạng thái, phân công và nhận đơn đều được ghi vào `order_status_history`.
- UI vận hành gồm `/admin/orders`, `/staff` và `/delivery`.

Giai đoạn 7 đã hoàn thiện review, liên hệ và thông báo:

- Khách hàng chỉ có thể đánh giá sản phẩm đã nhận hàng; mỗi tài khoản có một đánh giá
  cho mỗi sản phẩm và có thể cập nhật lại nội dung.
- Khách hoặc khách hàng đăng nhập có thể gửi yêu cầu hỗ trợ tại `/contact`; tài khoản
  đăng nhập xem được lịch sử yêu cầu và phản hồi.
- Admin/staff có màn hình xử lý liên hệ tại `/admin/contacts` và `/staff/contacts`, hỗ trợ
  lọc trạng thái, phản hồi và đánh dấu đã xử lý.
- Thông báo được tạo cho các sự kiện đơn hàng, giao hàng và liên hệ; người dùng xem và
  đánh dấu đã đọc từ menu thông báo trên header.

Giai đoạn 8 bổ sung đánh giá theo đơn hàng và GreenFarm Rewards:

- Khi đơn đã giao/hoàn tất, khách có thể đánh giá trực tiếp từng sản phẩm ngay trong
  chi tiết đơn hàng.
- Khách nhận điểm sau đơn giao thành công và sau mỗi đánh giá hợp lệ; lịch sử điểm có
  tại `/points`.
- Checkout cho phép dùng điểm để giảm giá, tối đa 50% giá trị thanh toán; một điểm đổi
  100đ, và mỗi 10.000đ giá trị hợp lệ nhận một điểm.

## Cấu trúc

- `frontend/`: Next.js 16, React 19 và TypeScript.
- `backend/`: Spring Boot 4, Java 21, Spring Security và JPA.
- `database/`: hướng dẫn sử dụng MySQL và dump cục bộ.
- `docs/`: tài liệu kỹ thuật và hợp đồng API.

## Chạy local

Yêu cầu: Docker Desktop và Node.js.

```powershell
docker compose up -d --build
cd frontend
npm run dev
```

Các địa chỉ local:

- Frontend: `http://localhost:3000`
- Backend health: `http://localhost:8080/api/health`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- MySQL từ máy host: `localhost:3307`
- MySQL trong Docker network: `mysql:3306`

Các cổng đăng nhập frontend:

- Khách hàng: `http://localhost:3000/login` → `/`
- Nhân viên: `http://localhost:3000/staff/login` → `/staff`
- Giao hàng: `http://localhost:3000/delivery/login` → `/delivery`
- Quản trị viên: `http://localhost:3000/admin/login` → `/admin`

Docker Compose dùng mật khẩu MySQL mặc định `greenfarm` cho môi trường local.
Có thể ghi đè các giá trị trong `.env` dựa trên [.env.example](.env.example).

## Kiểm tra chất lượng

```powershell
docker compose build backend
cd frontend
npm run quality
```

Image backend chỉ được tạo khi toàn bộ unit test Maven vượt qua.

## Tài liệu

- [Module xác thực và tài khoản](docs/api/authentication.md)
- [Module catalog sản phẩm](docs/api/catalog.md)
- [Module giỏ hàng và yêu thích](docs/api/cart-wishlist.md)
- [Module checkout và đơn hàng](docs/api/orders.md)
- [Module thanh toán](docs/api/payments.md)
- [Quy trình vận hành đơn](docs/api/order-operations.md)
- [Review, liên hệ và thông báo](docs/api/engagement.md)
- [Điểm tích lũy và đổi điểm](docs/api/loyalty.md)
- [Database local](database/README.md)
