CREATE TABLE vnpay_refunds (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    payment_id BIGINT UNSIGNED NOT NULL,
    request_id VARCHAR(32) NOT NULL,
    refund_amount DECIMAL(15,2) NOT NULL,
    transaction_type CHAR(2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    response_code VARCHAR(10) NULL,
    transaction_status VARCHAR(10) NULL,
    refund_transaction_id VARCHAR(100) NULL,
    requested_by VARCHAR(245) NOT NULL,
    response_payload TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_vnpay_refunds_request_id (request_id),
    KEY idx_vnpay_refunds_payment_status (payment_id, status),
    CONSTRAINT vnpay_refunds_payment_fk FOREIGN KEY (payment_id) REFERENCES payments(id)
);
