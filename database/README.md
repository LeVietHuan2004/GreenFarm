# Database

Môi trường local dùng MySQL 8 với database `veggie_main`.

## Baseline cũ

Dump của dự án trước được đặt tại `database/local/veggie-main.sql`. File này có
dữ liệu lịch sử nên được Git bỏ qua.

Docker chỉ import dump khi volume `greenfarm_mysql_data` được tạo lần đầu. Việc
khởi động lại container không import lại và không xóa dữ liệu trong volume.

## Migration

Schema cũ được Flyway đánh dấu là baseline phiên bản 1. Mọi thay đổi mới nằm tại
`backend/src/main/resources/db/migration`:

- V1: baseline của dump cũ, không có file migration vật lý.
- V2: thêm bộ đếm đăng nhập sai, thời gian khóa, lần đăng nhập cuối và unique key
  cho `role_permissions`.
- V3: thêm index phục vụ tìm kiếm/lọc catalog theo trạng thái, danh mục và giá.
- V4: ràng buộc duy nhất theo tài khoản/sản phẩm cho `cart_items`, `wishlists`
  và số lượng giỏ hàng phải lớn hơn 0.
- V5: snapshot người nhận và sản phẩm trên đơn hàng, các check constraint tiền/
  số lượng, index lịch sử đơn và bảo vệ địa chỉ/sản phẩm đã được dùng trong đơn.

Khi backend khởi động, Flyway tự kiểm tra và chỉ chạy migration chưa được áp
dụng. Không chỉnh trực tiếp một migration đã chạy; hãy tạo phiên bản mới.

## Kết nối

- Backend trong Docker dùng `mysql:3306`.
- Công cụ trên máy như MySQL Workbench dùng `localhost:3307`.
- Database, user và password local mặc định là `veggie_main`, `root`,
  `greenfarm`.
