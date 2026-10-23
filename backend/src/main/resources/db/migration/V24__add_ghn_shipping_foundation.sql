ALTER TABLE products
    ADD COLUMN ghn_eligible BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN package_weight_g INT NULL,
    ADD COLUMN package_length_cm INT NULL,
    ADD COLUMN package_width_cm INT NULL,
    ADD COLUMN package_height_cm INT NULL;

ALTER TABLE shipping_addresses
    ADD COLUMN ghn_province_id INT NULL,
    ADD COLUMN ghn_province_name VARCHAR(100) NULL,
    ADD COLUMN ghn_ward_id INT NULL,
    ADD COLUMN ghn_ward_name VARCHAR(100) NULL;

ALTER TABLE orders
    ADD COLUMN shipping_provider VARCHAR(20) NOT NULL DEFAULT 'internal',
    ADD COLUMN shipping_province_id INT NULL,
    ADD COLUMN shipping_province_name VARCHAR(100) NULL,
    ADD COLUMN shipping_ward_id INT NULL,
    ADD COLUMN shipping_ward_name VARCHAR(100) NULL;

ALTER TABLE order_items
    ADD COLUMN package_weight_g INT NULL,
    ADD COLUMN package_length_cm INT NULL,
    ADD COLUMN package_width_cm INT NULL,
    ADD COLUMN package_height_cm INT NULL;

CREATE TABLE shipments (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT UNSIGNED NOT NULL,
    provider VARCHAR(20) NOT NULL,
    provider_order_code VARCHAR(64) NULL,
    client_order_code VARCHAR(50) NOT NULL,
    shop_id INT NOT NULL,
    customer_shipping_fee DECIMAL(12, 2) NOT NULL DEFAULT 0,
    carrier_shipping_fee DECIMAL(12, 2) NULL,
    cod_amount DECIMAL(12, 2) NOT NULL DEFAULT 0,
    shipping_status VARCHAR(50) NOT NULL DEFAULT 'not_created',
    shipping_service_type INT NULL,
    estimated_delivery_at DATETIME NULL,
    last_event_at DATETIME NULL,
    cod_transferred_at DATETIME NULL,
    last_error VARCHAR(1000) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_shipments_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE RESTRICT,
    CONSTRAINT uk_shipments_order UNIQUE (order_id),
    CONSTRAINT uk_shipments_client_order_code UNIQUE (client_order_code),
    CONSTRAINT uk_shipments_provider_order_code UNIQUE (provider_order_code),
    INDEX idx_shipments_status (provider, shipping_status)
);

CREATE TABLE shipment_events (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    shipment_id BIGINT UNSIGNED NOT NULL,
    event_key VARCHAR(150) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    shipping_status VARCHAR(50) NULL,
    event_at DATETIME NULL,
    description VARCHAR(1000) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_shipment_events_shipment FOREIGN KEY (shipment_id) REFERENCES shipments(id) ON DELETE RESTRICT,
    CONSTRAINT uk_shipment_events_key UNIQUE (shipment_id, event_key),
    INDEX idx_shipment_events_order (shipment_id, event_at)
);
