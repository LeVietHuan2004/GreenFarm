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

## Order and coupon operations

- `PATCH /api/orders/{id}/cancel` lets a customer cancel a pending, unpaid order.
- Admins with `manage_orders` can list orders at `GET /api/admin/orders` and move
  one allowed status at `PATCH /api/admin/orders/{id}/status`.
- COD changes to completed automatically when an order is marked `delivered`.
- Admins with `manage_coupons` can create, update, activate and list coupons at
  `/api/admin/coupons`.
