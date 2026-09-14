# Review, liên hệ và thông báo — Giai đoạn 7

## Review sản phẩm

Danh sách review là API công khai. Tạo/cập nhật review yêu cầu JWT của khách hàng.
Khách chỉ được review sản phẩm thuộc đơn ở trạng thái `delivered` hoặc `completed`;
mỗi tài khoản chỉ có một review cho mỗi sản phẩm.

| Method | Endpoint | Chức năng |
| --- | --- | --- |
| GET | `/api/public/products/{productId}/reviews` | Danh sách review phân trang |
| GET | `/api/reviews/eligibility/{productId}` | Kiểm tra đã mua và review hiện có |
| POST | `/api/reviews` | Tạo review |
| PUT | `/api/reviews/{id}` | Cập nhật review của chính người dùng |

Body tạo/cập nhật:

```json
{
  "productId": 12,
  "rating": 5,
  "comment": "Rau tươi, đóng gói cẩn thận."
}
```

`rating` từ 1 đến 5, `comment` không bắt buộc và tối đa 1000 ký tự.

## Liên hệ hỗ trợ

Khách chưa đăng nhập vẫn có thể gửi liên hệ. Nếu request có JWT hợp lệ, liên hệ được
gắn với tài khoản để khách xem lịch sử và nhận thông báo khi có phản hồi.

| Method | Endpoint | Chức năng |
| --- | --- | --- |
| POST | `/api/public/contacts` | Gửi yêu cầu hỗ trợ |
| GET | `/api/contacts/mine` | Lịch sử hỗ trợ của tài khoản hiện tại |
| GET | `/api/operations/contacts` | Danh sách phân trang, lọc bằng `status` |
| PATCH | `/api/operations/contacts/{id}/reply` | Phản hồi yêu cầu |
| PATCH | `/api/operations/contacts/{id}/resolve` | Đánh dấu đã xử lý |

Các endpoint `/api/operations/contacts` yêu cầu quyền `manage_contacts`. Trạng thái hợp
lệ gồm `open`, `replied`, `resolved`.

Body gửi liên hệ:

```json
{
  "fullName": "Nguyễn Văn A",
  "phoneNumber": "0901234567",
  "email": "a@example.com",
  "message": "Tôi cần hỗ trợ về đơn hàng."
}
```

Nội dung liên hệ dài từ 10 đến 2000 ký tự.

Body phản hồi:

```json
{ "response": "GreenFarm đã kiểm tra và sẽ liên hệ lại với bạn." }
```

## Thông báo

Mọi endpoint thông báo yêu cầu JWT và chỉ thao tác trên thông báo thuộc tài khoản hiện tại.

| Method | Endpoint | Chức năng |
| --- | --- | --- |
| GET | `/api/notifications` | Danh sách thông báo phân trang |
| GET | `/api/notifications/unread-count` | Tổng số thông báo chưa đọc |
| PATCH | `/api/notifications/{id}/read` | Đánh dấu một thông báo đã đọc |
| PATCH | `/api/notifications/read-all` | Đánh dấu tất cả đã đọc |

Thông báo được phát sinh khi tạo/chuyển trạng thái/phân công/giao đơn, khi có liên hệ mới
cho admin/staff, và khi yêu cầu hỗ trợ của khách hàng được phản hồi hoặc xử lý.
