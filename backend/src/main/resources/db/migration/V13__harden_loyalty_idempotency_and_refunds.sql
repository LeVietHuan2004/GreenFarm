ALTER TABLE payments
    MODIFY COLUMN status ENUM('pending', 'completed', 'failed', 'refunded') NOT NULL DEFAULT 'pending';

ALTER TABLE loyalty_point_transactions
    ADD COLUMN product_id BIGINT UNSIGNED NULL AFTER review_id;

UPDATE loyalty_point_transactions transaction_history
JOIN reviews review ON review.id = transaction_history.review_id
SET transaction_history.product_id = review.product_id
WHERE transaction_history.type = 'review_earn';

ALTER TABLE loyalty_point_transactions
    DROP FOREIGN KEY loyalty_transactions_review_fk;

ALTER TABLE loyalty_point_transactions
    ADD CONSTRAINT loyalty_transactions_review_fk
        FOREIGN KEY (review_id) REFERENCES reviews(id) ON DELETE SET NULL,
    ADD CONSTRAINT loyalty_transactions_product_fk
        FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE SET NULL,
    ADD UNIQUE KEY uq_loyalty_user_product_type (user_id, product_id, type);

ALTER TABLE users
    ADD CONSTRAINT chk_users_loyalty_points_nonnegative CHECK (loyalty_points_balance >= 0);
