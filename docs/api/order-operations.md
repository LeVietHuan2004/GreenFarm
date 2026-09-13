# Quy trình vận hành đơn — Giai đoạn 6

Các endpoint dưới đây yêu cầu JWT và kiểm tra vai trò/quyền trên backend.

## Luồng trạng thái

```text
pending -> processing -> ready_for_delivery -> out_for_delivery -> delivered -> completed
   |            |                |                    |
   +------------+----------------+                    +-> delivery_failed
                |                                         |
                +---------------> canceled                +-> ready_for_delivery
```

- Đơn VNPAY chỉ được xác nhận sau khi thanh toán thành công.
- Đơn COD được ghi nhận thanh toán khi chuyển sang `delivered`.
- Khi hủy đơn chưa thanh toán, tồn kho và lượt dùng coupon được hoàn lại đúng một lần.
- Sau `delivery_failed`, staff có thể đưa đơn về `ready_for_delivery`; phân công và
  thời điểm nhận đơn cũ được xóa để thực hiện lần giao mới.

## Admin

| Method | Endpoint | Chức năng |
| --- | --- | --- |
| GET | `/api/admin/orders` | Danh sách phân trang, có lọc theo trạng thái |
| GET | `/api/admin/orders/{id}` | Chi tiết và lịch sử đơn |
| PATCH | `/api/admin/orders/{id}/status` | Chuyển trạng thái hợp lệ |
| PATCH | `/api/admin/orders/{id}/delivery-staff` | Phân công delivery staff đang hoạt động |

Body phân công:

```json
{ "deliveryStaffId": 6 }
```

Chỉ đơn `ready_for_delivery` được phân công. Phân công lại sẽ xóa mốc nhận đơn cũ.

## Staff

Yêu cầu quyền `manage_orders`.

| Method | Endpoint | Chức năng |
| --- | --- | --- |
| GET | `/api/staff/orders` | Các đơn cần xử lý |
| PATCH | `/api/staff/orders/{id}/status` | Xác nhận, sẵn sàng giao, từ chối hoặc chuẩn bị giao lại |

Staff chỉ được chọn `processing`, `ready_for_delivery` hoặc `canceled`; bảng chuyển
trạng thái chung vẫn được kiểm tra nên không thể bỏ qua một bước.

## Delivery staff

Yêu cầu quyền `manage_deliveries`. Danh sách chỉ trả về đơn được phân công cho tài
khoản đang đăng nhập.

| Method | Endpoint | Chức năng |
| --- | --- | --- |
| GET | `/api/delivery/orders` | Danh sách đơn được phân công |
| POST | `/api/delivery/orders/{id}/claim` | Nhận đơn và ghi `deliveryClaimedAt` |
| PATCH | `/api/delivery/orders/{id}/status` | Cập nhật `out_for_delivery`, `delivered` hoặc `delivery_failed` |

Delivery staff phải gọi `claim` trước khi chuyển sang `out_for_delivery`. Chỉ người
đã được admin phân công mới được nhận và cập nhật đơn. Thao tác nhận đơn có tính idempotent,
không tạo lịch sử trùng nếu gửi lại request.

Body cập nhật trạng thái:

```json
{
  "status": "delivery_failed",
  "note": "Khách không nghe máy"
}
```

Response chi tiết đơn chứa `deliveryStaffId`, `deliveryStaffName`,
`deliveryClaimedAt`, `deliveryFailureReason` và toàn bộ `statusHistory`.
