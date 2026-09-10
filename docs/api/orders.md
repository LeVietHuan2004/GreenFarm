# Địa chỉ, checkout và đơn hàng — Giai đoạn 4

Tất cả endpoint yêu cầu JWT của khách hàng. `userId` luôn lấy từ token.

| Method | Endpoint | Chức năng |
| --- | --- | --- |
| GET | `/api/shipping-addresses` | Danh sách địa chỉ |
| POST | `/api/shipping-addresses` | Thêm địa chỉ |
| PUT | `/api/shipping-addresses/{id}` | Sửa địa chỉ thuộc tài khoản |
| PATCH | `/api/shipping-addresses/{id}/default` | Đặt mặc định |
| DELETE | `/api/shipping-addresses/{id}` | Xóa địa chỉ chưa được dùng trong đơn |
| POST | `/api/checkout/preview` | Tính lại toàn bộ số tiền |
| POST | `/api/orders` | Tạo đơn từ giỏ hiện tại |
| GET | `/api/orders` | Danh sách đơn của khách hàng |
| GET | `/api/orders/{id}` | Chi tiết và lịch sử trạng thái |

Body cho preview và tạo đơn:

```json
{
  "shippingAddressId": 1,
  "couponCode": "FREESHIP"
}
```

`couponCode` là tùy chọn. Backend hỗ trợ coupon `ORDER_DISCOUNT` theo phần trăm
hoặc số tiền cố định và coupon `FREESHIP`. Coupon phải đang hoạt động, nằm trong
thời gian hiệu lực và chưa hết lượt. Dữ liệu hiện tại không có mức đơn tối thiểu
hoặc mức giảm tối đa nên hai quy tắc đó chưa được áp dụng.

## Tính tiền

- `subtotal`: tổng giá hiện tại × số lượng của từng dòng giỏ.
- `shippingFee`: 30.000đ; bằng 0 khi subtotal từ 500.000đ hoặc có coupon freeship.
- `discountAmount`: mức giảm coupon, không vượt subtotal.
- `total`: `subtotal - discountAmount + shippingFee`.

Client chỉ hiển thị kết quả. Preview và tạo đơn đều tính lại trên backend.

## Transaction tạo đơn

1. Khóa tài khoản để tuần tự hóa thao tác checkout cùng một giỏ.
2. Khóa tất cả sản phẩm theo ID tăng dần để tránh bán vượt kho và giảm deadlock.
3. Kiểm tra trạng thái, tồn kho, địa chỉ thuộc tài khoản và coupon.
4. Lưu order, snapshot người nhận, order items và snapshot tên/đơn vị/ảnh/giá.
5. Trừ tồn kho; sản phẩm về 0 chuyển sang `out_of_stock`.
6. Lưu trạng thái `pending` đầu tiên vào `order_status_history`.
7. Tăng lượt dùng coupon và xóa giỏ.

Mọi bước cùng một transaction; lỗi bất kỳ bước nào rollback toàn bộ. Địa chỉ và
sản phẩm đã nằm trong đơn không thể bị xóa vật lý để bảo toàn lịch sử. Thông tin
hiển thị của đơn lấy từ snapshot nên việc sửa địa chỉ hoặc sản phẩm sau đó không
làm thay đổi đơn cũ.
