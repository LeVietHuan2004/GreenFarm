ALTER TABLE payments
    ADD COLUMN reference_code VARCHAR(100) NULL AFTER payment_method,
    ADD COLUMN gateway_response_code VARCHAR(20) NULL AFTER transaction_id,
    ADD COLUMN gateway_payload TEXT NULL AFTER gateway_response_code,
    ADD COLUMN expires_at TIMESTAMP NULL AFTER paid_at;

CREATE UNIQUE INDEX uk_payments_reference_code ON payments(reference_code);
CREATE INDEX idx_payments_order_status ON payments(order_id, status);
