ALTER TABLE users
    ADD COLUMN loyalty_points_balance INT NOT NULL DEFAULT 0 AFTER last_login_at;

ALTER TABLE orders
    ADD COLUMN loyalty_points_used INT NOT NULL DEFAULT 0 AFTER shipping_fee,
    ADD COLUMN loyalty_discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0 AFTER loyalty_points_used;

CREATE TABLE loyalty_point_transactions (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    order_id BIGINT UNSIGNED NULL,
    review_id BIGINT UNSIGNED NULL,
    type VARCHAR(30) NOT NULL,
    points INT NOT NULL,
    description VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_loyalty_order_type (order_id, type),
    UNIQUE KEY uq_loyalty_review_type (review_id, type),
    KEY idx_loyalty_user_created (user_id, created_at),
    CONSTRAINT loyalty_transactions_user_fk FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT loyalty_transactions_order_fk FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT loyalty_transactions_review_fk FOREIGN KEY (review_id) REFERENCES reviews(id) ON DELETE CASCADE
);
