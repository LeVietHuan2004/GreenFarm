# Coupon / Voucher API

## Quy tắc tính tiền

Backend luôn đọc lại giá sản phẩm, giỏ hàng, phí giao hàng, coupon và số dư điểm. Frontend chỉ gửi `couponCode`, `freeShippingCouponCode` và `loyaltyPoints`; mọi số tiền giảm đều do backend tính theo thứ tự:

```text
tạm tính
− coupon sản phẩm/đơn hàng
− giảm từ điểm
+ phí giao hàng
− coupon phí vận chuyển
= tổng thanh toán
```

Mỗi đơn dùng tối đa một coupon `ORDER_DISCOUNT` và một coupon `FREESHIP`. Coupon và điểm GreenFarm Rewards được dùng cùng nhau. Với scope `CATEGORY` hoặc `PRODUCT`, coupon sản phẩm chỉ tính trên phần tạm tính của các dòng hàng phù hợp.

## Checkout

- `GET /api/public/coupons`: trả về các mã đang bật, đã bắt đầu, chưa hết hạn và còn lượt dùng chung để gợi ý tại ô nhập mã. Kết quả gồm loại `ORDER_DISCOUNT` hoặc `FREESHIP`, giá trị ưu đãi và điều kiện tối thiểu. Danh sách này không xác nhận giới hạn theo khách hoặc sản phẩm trong giỏ; bước preview và đặt hàng vẫn kiểm tra đầy đủ.

- `POST /api/checkout/preview`: kiểm tra và trả về toàn bộ breakdown nhưng chưa giữ lượt coupon.
- `POST /api/orders`: khóa coupon, kiểm tra lại điều kiện và tạo reservation trong cùng transaction với order.

Ví dụ body:

```json
{
  "shippingAddressId": 12,
  "couponCode": "RAU20",
  "freeShippingCouponCode": "FREESHIP",
  "paymentMethod": "vnpay",
  "loyaltyPoints": 15000
}
```

Coupon hỗ trợ giới hạn thời gian, giá trị đơn tối thiểu, tổng lượt dùng, lượt dùng mỗi khách, giảm tối đa và trạng thái bật/tắt. VNPAY nhận chính `total` cuối cùng đã được backend tính và lưu trên order/payment.

## Vòng đời lượt sử dụng

- Tạo order thành công: `RESERVED`.
- VNPAY thành công hoặc order chuyển sang chuẩn bị hàng: `USED`.
- Hủy trước khi hoàn tất: `RELEASED`, trả lại đúng một lượt.
- Order đã hoàn tất sau đó refund: giữ `USED`, không phát hành lại coupon.

Các lần callback/chuyển trạng thái/hủy lặp được xử lý idempotent. Pessimistic lock trên coupon và unique key `(coupon_id, order_id)` bảo vệ giới hạn khi checkout đồng thời.

## Admin

Các API yêu cầu quyền `manage_coupons`:

- `GET /api/admin/coupons`
- `GET /api/admin/coupons/{id}`
- `POST /api/admin/coupons`
- `PUT /api/admin/coupons/{id}`
- `PATCH /api/admin/coupons/{id}/active`
- `DELETE /api/admin/coupons/{id}` (chỉ coupon chưa có lịch sử)
- `GET /api/admin/coupons/{id}/usages`

Sau khi có lịch sử sử dụng, loại coupon, cách/giá trị giảm, mức giảm tối đa và scope không thể thay đổi. Admin vẫn có thể tạm dừng và chỉnh các thông tin vận hành không làm thay đổi quyền lợi đã ghi nhận.
