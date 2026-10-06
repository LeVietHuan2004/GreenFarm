# Payment API

## Payment methods

`GET /api/payments/methods` requires a customer access token. It returns whether COD
and VNPAY are currently available. COD is always available. VNPAY is available only
when the merchant code, hash secret and public return URL are configured.

## Create an order with payment

`POST /api/orders` accepts `paymentMethod` in addition to the Phase 4 fields:

```json
{
  "shippingAddressId": 12,
  "couponCode": "FREESHIP",
  "paymentMethod": "cod"
}
```

Accepted values are `cod` and `vnpay`. A COD order receives a pending payment record.
For VNPAY, the response contains `payment.paymentUrl`; the client redirects the
customer to that URL.

## Hóa đơn trên web và email

Khách được chuyển đến `/orders/{id}/invoice` ngay sau khi tạo đơn COD hoặc sau khi
VNPAY xác nhận thanh toán. Trang hóa đơn chỉ dùng được bởi chủ đơn, hiển thị đầy đủ
sản phẩm, địa chỉ, thanh toán, giảm giá và tổng tiền; người dùng có thể in/lưu PDF
ngay từ trình duyệt.

Backend gửi một hóa đơn HTML đến email đăng ký của khách hàng:

- COD: gửi sau khi đơn được tạo thành công (thể hiện thanh toán khi nhận hàng).
- VNPAY: chỉ gửi khi callback có chữ ký hợp lệ xác nhận thanh toán thành công.
- Mốc gửi được lưu trên payment để callback lặp không gửi trùng.

Tính năng gửi email được tắt mặc định để local development không cố gắng gửi SMTP.
Thiết lập các biến sau trong `.env` để bật:

```dotenv
STOREFRONT_URL=https://your-domain.example
INVOICE_EMAIL_ENABLED=true
INVOICE_EMAIL_FROM=billing@your-domain.example
MAIL_HOST=smtp.example.com
MAIL_PORT=587
MAIL_USERNAME=billing@your-domain.example
MAIL_PASSWORD=your-smtp-app-password
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS_ENABLE=true
```

Lỗi SMTP không làm đơn hàng hoặc thanh toán đã xác nhận bị rollback. Payment lưu số
lần thử, lỗi gần nhất và thời điểm thử lại; scheduler retry theo exponential backoff
tối đa 5 lần (cấu hình qua `INVOICE_EMAIL_MAX_ATTEMPTS`,
`INVOICE_EMAIL_BASE_RETRY_MINUTES`, `INVOICE_EMAIL_RETRY_SCAN_MS`).

## VNPAY callbacks

- `GET /api/payments/vnpay/return` verifies the signed browser return and redirects
  to the frontend result page.
- `GET /api/payments/vnpay/ipn` verifies and records the server notification.

The callback verifies HMAC-SHA512, transaction reference and amount before updating
a payment. Configure `VNPAY_TMN_CODE`, `VNPAY_HASH_SECRET` and a public
`VNPAY_RETURN_URL` in `.env`; never expose the secret through frontend variables.

VNPAY payments expire after 15 minutes. The scheduled expiry check marks an unpaid
payment as failed, cancels its order, restores stock and returns the coupon usage
atomically. The same cancellation path handles a failed gateway response. For an
amount mismatch, IPN returns `RspCode=04`; it never acknowledges the payment as
successful.

## VNPAY refund

`PATCH /api/admin/orders/{id}/refund-confirmation` calls the configured VNPAY Refund
API for completed VNPAY payments. It uses the existing `VNPAY_TMN_CODE` and
`VNPAY_HASH_SECRET`; no credential is hard-coded and the Sandbox endpoint is the
default `VNPAY_REFUND_URL`.

```json
{ "note": "Khách trả hàng", "amount": 50000 }
```

Omit `amount` for a full refund. Supplying an amount lower than the remaining paid
amount submits a partial refund (`vnp_TransactionType=03`); a full refund uses type
`02` and only then cancels the order. Each request is saved in `vnpay_refunds` with
its unique request ID, amount, VNPAY transaction/status/response codes and raw
payload. A pending request blocks another refund for the same payment. The response
HMAC-SHA512 is verified before any payment or order state is changed. If VNPAY
returns a processing status, the order remains unchanged instead of being cancelled.
COD refunds remain an internal confirmed refund because there is no remote gateway.

## Order and coupon operations

- `PATCH /api/orders/{id}/cancel` lets a customer cancel a pending, unpaid order.
- Admins with `manage_orders` can list orders at `GET /api/admin/orders` and move
  one allowed status at `PATCH /api/admin/orders/{id}/status`.
- COD changes to completed automatically when an order is marked `delivered`.
- Admins with `manage_coupons` can create, update, activate and list coupons at
  `/api/admin/coupons`.
