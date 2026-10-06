ALTER TABLE payments
    ADD COLUMN vnpay_payment_request_date TIMESTAMP NULL AFTER expires_at;
