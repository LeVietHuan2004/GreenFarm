# Điểm tích lũy — Giai đoạn 8

## Quy tắc điểm thưởng

- Mỗi 10.000đ giá trị đơn hàng đã giao thành công nhận 1 điểm.
- Sau khi đánh giá sản phẩm từ đơn đã giao, khách nhận thêm điểm theo giá trị dòng sản
  phẩm đó (10.000đ = 1 điểm); mỗi review chỉ nhận một lần.
- 1 điểm đổi được 100đ khi checkout.
- Tối đa dùng điểm để giảm 50% số tiền thanh toán sau coupon/phí giao hàng.
- Điểm dùng cho đơn chưa thanh toán sẽ được hoàn lại nếu đơn bị hủy. Mọi thay đổi điểm
  được lưu trong `loyalty_point_transactions` để tránh cộng/trừ lặp.

## API

`GET /api/loyalty` yêu cầu JWT khách hàng, trả về số dư, chính sách quy đổi và 30 giao
dịch gần nhất.

Ví dụ response:

```json
{
  "pointsBalance": 125,
  "discountPerPoint": 100,
  "earnAmountPerPoint": 10000,
  "maxRedemptionPercent": 50,
  "transactions": []
}
```

Khi preview hoặc tạo đơn, truyền thêm `loyaltyPoints` vào body checkout:

```json
{
  "shippingAddressId": 12,
  "couponCode": "FREESHIP",
  "paymentMethod": "cod",
  "loyaltyPoints": 50
}
```

Response checkout trả thêm `loyaltyPointsApplied` và `loyaltyDiscountAmount`; backend
luôn kiểm tra số dư và giới hạn giảm giá, không tin số tiền do frontend tự tính.
