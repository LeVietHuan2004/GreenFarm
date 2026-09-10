ALTER TABLE orders
    ADD COLUMN recipient_name VARCHAR(255) NULL AFTER shipping_address_id,
    ADD COLUMN recipient_phone VARCHAR(30) NULL AFTER recipient_name,
    ADD COLUMN shipping_address_line VARCHAR(255) NULL AFTER recipient_phone,
    ADD COLUMN shipping_city VARCHAR(255) NULL AFTER shipping_address_line;

UPDATE orders o
JOIN shipping_addresses a ON a.id = o.shipping_address_id
SET o.recipient_name = a.full_name,
    o.recipient_phone = a.phone,
    o.shipping_address_line = a.address,
    o.shipping_city = a.city
WHERE o.recipient_name IS NULL;

ALTER TABLE order_items
    ADD COLUMN product_name VARCHAR(255) NULL AFTER product_id,
    ADD COLUMN product_unit VARCHAR(50) NULL AFTER product_name,
    ADD COLUMN product_image VARCHAR(500) NULL AFTER product_unit;

UPDATE order_items oi
JOIN products p ON p.id = oi.product_id
SET oi.product_name = p.name,
    oi.product_unit = p.unit,
    oi.product_image = (
        SELECT pi.image FROM product_images pi
        WHERE pi.product_id = p.id
        ORDER BY pi.id DESC LIMIT 1
    )
WHERE oi.product_name IS NULL;

ALTER TABLE orders
    MODIFY recipient_name VARCHAR(255) NOT NULL,
    MODIFY recipient_phone VARCHAR(30) NOT NULL,
    MODIFY shipping_address_line VARCHAR(255) NOT NULL,
    MODIFY shipping_city VARCHAR(255) NOT NULL,
    ADD CONSTRAINT chk_orders_amounts_non_negative CHECK (
        subtotal >= 0 AND discount_amount >= 0 AND shipping_fee >= 0 AND total_price >= 0
    );

ALTER TABLE order_items
    MODIFY product_name VARCHAR(255) NOT NULL,
    ADD CONSTRAINT chk_order_items_quantity_positive CHECK (quantity > 0),
    ADD CONSTRAINT chk_order_items_price_non_negative CHECK (price >= 0);

ALTER TABLE orders
    DROP FOREIGN KEY orders_shipping_address_id_foreign;

ALTER TABLE orders
    ADD CONSTRAINT orders_shipping_address_id_foreign
        FOREIGN KEY (shipping_address_id) REFERENCES shipping_addresses(id) ON DELETE RESTRICT;

ALTER TABLE order_items
    DROP FOREIGN KEY order_items_product_id_foreign;

ALTER TABLE order_items
    ADD CONSTRAINT order_items_product_id_foreign
        FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT;

CREATE INDEX idx_orders_user_created ON orders(user_id, created_at DESC);
CREATE INDEX idx_order_status_history_order_changed ON order_status_history(order_id, changed_at ASC);
