# Module xác thực và tài khoản

## Phạm vi giai đoạn 1

Module dùng access token JWT có thời hạn mặc định 24 giờ. Chưa có refresh token,
xác minh email, quên mật khẩu hoặc đăng nhập Google trong giai đoạn này.

Tài khoản đăng ký mới nhận role `customer` và trạng thái `active`. Mật khẩu được
băm bằng BCrypt trước khi lưu.

## Endpoint công khai

| Method | Endpoint | Chức năng |
| --- | --- | --- |
| `POST` | `/api/auth/register` | Đăng ký và nhận JWT |
| `POST` | `/api/auth/login` | Đăng nhập và nhận JWT |
| `GET` | `/api/health` | Kiểm tra backend |

Request đăng nhập có thể gửi thêm `role` để khóa phiên đăng nhập vào đúng cổng:

```json
{
  "email": "user@greenfarm.vn",
  "password": "your-password",
  "role": "customer"
}
```

Các giá trị role hợp lệ là `customer`, `staff`, `delivery_staff` và `admin`.
Nếu mật khẩu đúng nhưng role tài khoản không khớp cổng đã chọn, API trả `403`
với mã `ROLE_MISMATCH`. Trường `role` vẫn là tùy chọn để giữ tương thích với
client API cũ.

## Endpoint cần JWT

Header xác thực:

```http
Authorization: Bearer <access-token>
```

| Method | Endpoint | Chức năng |
| --- | --- | --- |
| `GET` | `/api/users/me` | Lấy hồ sơ hiện tại |
| `PATCH` | `/api/users/me` | Cập nhật hồ sơ |
| `PUT` | `/api/users/me/password` | Đổi mật khẩu |

## Endpoint quản trị

Các endpoint sau yêu cầu permission `manage_users`:

| Method | Endpoint | Chức năng |
| --- | --- | --- |
| `GET` | `/api/admin/users` | Tìm kiếm, lọc vai trò/trạng thái và phân trang người dùng |
| `PATCH` | `/api/admin/users/{userId}` | Đổi role hoặc trạng thái |
| `GET` | `/api/roles` | Danh sách role và permission |

Danh sách người dùng nhận các query tùy chọn `search`, `role`, `status`, `page`,
`size` và `sort`. Backend từ chối việc quản trị viên tự đổi vai trò hoặc trạng
thái của chính mình với mã `SELF_ACCOUNT_PROTECTED`, tránh tự làm mất quyền truy
cập khu vực quản trị.

## Vai trò hiện có

- `admin`: toàn bộ permission hiện có.
- `staff`: quản lý sản phẩm và liên hệ.
- `delivery_staff`: quản lý giao hàng.
- `customer`: không có permission quản trị.

## Bảo vệ đăng nhập

Mỗi lần nhập sai mật khẩu làm tăng bộ đếm thất bại. Lần sai thứ 5 khóa tài khoản
15 phút; đăng nhập đúng sẽ xóa bộ đếm. Các giá trị có thể cấu hình bằng
`LOGIN_MAX_ATTEMPTS` và `LOGIN_LOCK_DURATION_MINUTES`.

API trả lỗi theo một định dạng chung gồm `status`, `code`, `message`, `path` và
`fieldErrors`. Các mã chính gồm `INVALID_CREDENTIALS`, `ACCOUNT_LOCKED`,
`ROLE_MISMATCH`, `ACCESS_DENIED`, `EMAIL_EXISTS` và `VALIDATION_ERROR`.
