ALTER TABLE coupons
    ADD COLUMN name VARCHAR(150) NULL AFTER code,
    ADD COLUMN description VARCHAR(500) NULL AFTER name,
    ADD COLUMN max_discount_amount DECIMAL(12,2) NULL AFTER discount_amount,
    ADD COLUMN minimum_order_amount DECIMAL(12,2) NULL AFTER max_discount_amount,
    ADD COLUMN scope_type VARCHAR(20) NOT NULL DEFAULT 'ALL' AFTER minimum_order_amount,
    ADD COLUMN usage_limit_per_user INT UNSIGNED NULL AFTER usage_limit;

UPDATE coupons SET name = code WHERE name IS NULL;

ALTER TABLE coupons
    MODIFY COLUMN name VARCHAR(150) NOT NULL;

CREATE TABLE coupon_categories (
    coupon_id BIGINT UNSIGNED NOT NULL,
    category_id BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (coupon_id, category_id),
    KEY idx_coupon_categories_category (category_id),
    CONSTRAINT coupon_categories_coupon_fk FOREIGN KEY (coupon_id) REFERENCES coupons(id) ON DELETE CASCADE,
    CONSTRAINT coupon_categories_category_fk FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE
);

CREATE TABLE coupon_products (
    coupon_id BIGINT UNSIGNED NOT NULL,
    product_id BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (coupon_id, product_id),
    KEY idx_coupon_products_product (product_id),
    CONSTRAINT coupon_products_coupon_fk FOREIGN KEY (coupon_id) REFERENCES coupons(id) ON DELETE CASCADE,
    CONSTRAINT coupon_products_product_fk FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

CREATE TABLE coupon_usages (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    coupon_id BIGINT UNSIGNED NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,
    order_id BIGINT UNSIGNED NOT NULL,
    discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    used_at TIMESTAMP NULL,
    released_at TIMESTAMP NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_coupon_usage_coupon_order (coupon_id, order_id),
    KEY idx_coupon_usage_user_coupon_status (user_id, coupon_id, status),
    KEY idx_coupon_usage_order_status (order_id, status),
    CONSTRAINT coupon_usage_coupon_fk FOREIGN KEY (coupon_id) REFERENCES coupons(id) ON DELETE RESTRICT,
    CONSTRAINT coupon_usage_user_fk FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT coupon_usage_order_fk FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT chk_coupon_usage_discount_nonnegative CHECK (discount_amount >= 0),
    CONSTRAINT chk_coupon_usage_status CHECK (status IN ('RESERVED', 'USED', 'RELEASED'))
);

ALTER TABLE orders
    ADD COLUMN shipping_coupon_id BIGINT UNSIGNED NULL AFTER coupon_code,
    ADD COLUMN shipping_coupon_code VARCHAR(255) NULL AFTER shipping_coupon_id,
    ADD COLUMN shipping_discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0 AFTER shipping_coupon_code,
    ADD KEY idx_orders_shipping_coupon (shipping_coupon_id),
    ADD CONSTRAINT orders_shipping_coupon_fk FOREIGN KEY (shipping_coupon_id) REFERENCES coupons(id) ON DELETE SET NULL,
    ADD CONSTRAINT chk_orders_shipping_discount_nonnegative CHECK (shipping_discount_amount >= 0);
