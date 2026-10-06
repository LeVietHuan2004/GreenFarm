# Lưu trữ ảnh

## Luồng hiện tại

Các endpoint tải ảnh không đổi: `POST /api/admin/uploads/images` cho ảnh catalog,
`POST /api/admin/products/{productId}/images/upload` cho ảnh sản phẩm và
`POST /api/users/me/avatar` cho avatar. Ảnh phải là JPG, PNG hoặc WEBP và không
vượt 5 MB. Backend thực hiện upload, frontend chỉ nhận URL từ API. Không đưa
Cloudinary API secret ra frontend.

`ImageStorageService` chọn Cloudinary khi cả `CLOUDINARY_CLOUD_NAME`,
`CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET` đều có giá trị. Ảnh mới được tải
lên các thư mục `greenfarm/catalog`, `greenfarm/products`, `greenfarm/avatars`;
API trả về HTTPS `secure_url`. Nếu cả ba biến đều trống, service tiếp tục lưu
trong `UPLOAD_DIRECTORY`. Nếu chỉ có một phần cấu hình, backend báo lỗi khi
khởi động để tránh âm thầm lưu nhầm chỗ.

Thêm ba giá trị vào `.env` ở thư mục gốc khi chạy bằng Docker Compose. File
`compose.yml` chuyển chúng vào container backend. Khi chạy backend riêng, có
thể đặt chúng trong môi trường hoặc file `.env` mà Spring đang đọc. Không commit
file chứa API secret. Sau khi cấu hình, build và tạo lại container backend:

```powershell
docker compose up -d --build backend
```

## Ảnh cũ và database

`/uploads/**` và volume `greenfarm_uploads` vẫn được giữ để hiển thị ảnh local
trong dữ liệu cũ. URL Cloudinary có sẵn trong dữ liệu cũng tiếp tục hoạt động.
Migration V23 tăng giới hạn các cột URL ảnh danh mục, sản phẩm, avatar và ảnh
chụp trong đơn hàng lên 1024 ký tự.

Việc tải ảnh mới lên Cloudinary **không tự chuyển** ảnh local cũ. Trước khi bỏ
volume, sao lưu MySQL và volume ảnh, tải các ảnh local còn được tham chiếu lên
Cloudinary, cập nhật URL trong `categories`, `product_images`, `users` và
`order_items`, rồi kiểm tra catalog, hồ sơ và hóa đơn/đơn hàng lịch sử.

Xóa bản ghi ảnh sản phẩm hiện không tự xóa asset Cloudinary. Điều này tránh
làm hỏng ảnh đã được chụp vào đơn hàng cũ. Nếu sau này cần dọn asset, lưu
`public_id` từ kết quả upload và chỉ xóa khi không còn tham chiếu cần giữ.

## Kiểm tra

1. Tải một ảnh sản phẩm và một avatar sau khi cấu hình Cloudinary.
2. Xác nhận API trả URL bắt đầu bằng `https://res.cloudinary.com/` và ảnh hiển
   thị sau khi tải lại trang.
3. Xác nhận asset xuất hiện trong Cloudinary Media Library.
4. Mở ảnh cũ có đường dẫn `/uploads/...` và một đơn hàng cũ để kiểm tra tương
   thích dữ liệu lịch sử.
