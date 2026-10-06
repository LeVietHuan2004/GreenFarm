CREATE TABLE guest_sessions (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    token_hash CHAR(64) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    consumed_at TIMESTAMP NULL,
    merged_user_id BIGINT UNSIGNED NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_guest_sessions_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_guest_sessions_merged_user FOREIGN KEY (merged_user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_guest_sessions_expiry (expires_at)
);

CREATE TABLE guest_cart_items (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    guest_session_id BIGINT UNSIGNED NOT NULL,
    product_id BIGINT UNSIGNED NOT NULL,
    quantity INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_guest_cart_session_product UNIQUE (guest_session_id, product_id),
    CONSTRAINT chk_guest_cart_quantity_positive CHECK (quantity > 0),
    CONSTRAINT fk_guest_cart_session FOREIGN KEY (guest_session_id) REFERENCES guest_sessions(id) ON DELETE CASCADE,
    CONSTRAINT fk_guest_cart_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    INDEX idx_guest_cart_session (guest_session_id)
);

ALTER TABLE orders
    MODIFY user_id BIGINT UNSIGNED NULL,
    MODIFY shipping_address_id BIGINT UNSIGNED NULL,
    ADD COLUMN guest_session_id BIGINT UNSIGNED NULL AFTER user_id,
    ADD COLUMN guest_email VARCHAR(255) NULL AFTER recipient_phone,
    ADD COLUMN guest_lookup_token_hash CHAR(64) NULL AFTER guest_email,
    ADD COLUMN guest_checkout_key VARCHAR(64) NULL AFTER guest_lookup_token_hash,
    ADD COLUMN shipping_method VARCHAR(32) NOT NULL DEFAULT 'standard' AFTER shipping_city,
    ADD CONSTRAINT fk_orders_guest_session FOREIGN KEY (guest_session_id) REFERENCES guest_sessions(id) ON DELETE SET NULL,
    ADD CONSTRAINT uk_orders_guest_checkout UNIQUE (guest_session_id, guest_checkout_key),
    ADD INDEX idx_orders_guest_lookup (id, guest_email);

ALTER TABLE coupon_usages
    MODIFY user_id BIGINT UNSIGNED NULL,
    ADD COLUMN guest_session_id BIGINT UNSIGNED NULL AFTER user_id,
    ADD CONSTRAINT fk_coupon_usages_guest_session FOREIGN KEY (guest_session_id) REFERENCES guest_sessions(id) ON DELETE SET NULL,
    ADD INDEX idx_coupon_usages_guest_coupon (guest_session_id, coupon_id, status);
