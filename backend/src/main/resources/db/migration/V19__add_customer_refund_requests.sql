CREATE TABLE refund_requests (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    order_id BIGINT UNSIGNED NOT NULL,
    reason VARCHAR(100) NOT NULL,
    details VARCHAR(1000) NULL,
    status VARCHAR(30) NOT NULL,
    admin_note VARCHAR(1000) NULL,
    reviewed_by VARCHAR(255) NULL,
    reviewed_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_refund_requests_order FOREIGN KEY (order_id) REFERENCES orders(id),
    INDEX idx_refund_requests_order (order_id, id),
    INDEX idx_refund_requests_status (status, created_at)
);
