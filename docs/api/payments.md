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
