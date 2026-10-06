ALTER TABLE payments
    ADD COLUMN refund_request_id VARCHAR(64) NULL AFTER gateway_payload,
    ADD COLUMN refund_gateway_transaction_id VARCHAR(100) NULL AFTER refund_request_id,
    ADD COLUMN refund_requested_at TIMESTAMP NULL AFTER refund_gateway_transaction_id,
    ADD UNIQUE KEY uq_payments_refund_request_id (refund_request_id);
