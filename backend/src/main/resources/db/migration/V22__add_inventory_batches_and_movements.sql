CREATE TABLE suppliers (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    supplier_code VARCHAR(40) NOT NULL,
    name VARCHAR(150) NOT NULL,
    phone VARCHAR(30) NULL,
    email VARCHAR(255) NULL,
    address VARCHAR(500) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    note VARCHAR(1000) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_suppliers_code (supplier_code),
    KEY idx_suppliers_name (name),
    CONSTRAINT chk_suppliers_status CHECK (status IN ('active', 'inactive'))
);

CREATE TABLE inventory_batches (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    batch_code VARCHAR(64) NOT NULL,
    product_id BIGINT UNSIGNED NOT NULL,
    quantity INT NOT NULL,
    remaining_quantity INT NOT NULL,
    reserved_quantity INT NOT NULL DEFAULT 0,
    unit VARCHAR(255) NULL,
    import_price DECIMAL(12,2) NOT NULL DEFAULT 0,
    manufacture_date DATE NULL,
    expiry_date DATE NULL,
    supplier_id BIGINT UNSIGNED NULL,
    imported_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    note VARCHAR(1000) NULL,
    created_by BIGINT UNSIGNED NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_inventory_batches_code (batch_code),
    KEY idx_inventory_batches_product_expiry (product_id, expiry_date, id),
    KEY idx_inventory_batches_expiry (expiry_date),
    KEY idx_inventory_batches_supplier (supplier_id),
    CONSTRAINT fk_inventory_batches_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT,
    CONSTRAINT fk_inventory_batches_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(id) ON DELETE SET NULL,
    CONSTRAINT fk_inventory_batches_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT chk_inventory_batch_quantities CHECK (quantity >= 0 AND remaining_quantity >= 0 AND reserved_quantity >= 0 AND reserved_quantity <= remaining_quantity),
    CONSTRAINT chk_inventory_batch_price CHECK (import_price >= 0),
    CONSTRAINT chk_inventory_batch_dates CHECK (manufacture_date IS NULL OR expiry_date IS NULL OR expiry_date > manufacture_date)
);

CREATE TABLE order_inventory_allocations (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    order_item_id BIGINT UNSIGNED NOT NULL,
    batch_id BIGINT UNSIGNED NOT NULL,
    quantity INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    exported_at TIMESTAMP NULL,
    restored_at TIMESTAMP NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_inventory_item_batch (order_item_id, batch_id),
    KEY idx_order_inventory_batch_status (batch_id, status),
    CONSTRAINT fk_order_inventory_item FOREIGN KEY (order_item_id) REFERENCES order_items(id) ON DELETE CASCADE,
    CONSTRAINT fk_order_inventory_batch FOREIGN KEY (batch_id) REFERENCES inventory_batches(id) ON DELETE RESTRICT,
    CONSTRAINT chk_order_inventory_quantity CHECK (quantity > 0),
    CONSTRAINT chk_order_inventory_status CHECK (status IN ('RESERVED', 'EXPORTED', 'RELEASED', 'RESTORED'))
);

CREATE TABLE inventory_transactions (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    product_id BIGINT UNSIGNED NOT NULL,
    batch_id BIGINT UNSIGNED NOT NULL,
    type VARCHAR(30) NOT NULL,
    quantity INT NOT NULL,
    quantity_before INT NOT NULL,
    quantity_after INT NOT NULL,
    reserved_before INT NOT NULL,
    reserved_after INT NOT NULL,
    reference_id BIGINT UNSIGNED NULL,
    reason VARCHAR(1000) NULL,
    created_by BIGINT UNSIGNED NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_inventory_transactions_product_created (product_id, created_at, id),
    KEY idx_inventory_transactions_batch_created (batch_id, created_at, id),
    KEY idx_inventory_transactions_type_created (type, created_at, id),
    CONSTRAINT fk_inventory_transactions_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT,
    CONSTRAINT fk_inventory_transactions_batch FOREIGN KEY (batch_id) REFERENCES inventory_batches(id) ON DELETE RESTRICT,
    CONSTRAINT fk_inventory_transactions_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT chk_inventory_transaction_balances CHECK (quantity_before >= 0 AND quantity_after >= 0 AND reserved_before >= 0 AND reserved_after >= 0)
);

-- Preserve every existing Product.stock value. Pending orders are reservations;
-- older orders already past PENDING are represented as exported allocations.
INSERT INTO inventory_batches (batch_code, product_id, quantity, remaining_quantity, reserved_quantity, unit, import_price, note)
SELECT CONCAT('LEGACY-', p.id), p.id,
       p.stock + COALESCE((SELECT SUM(oi.quantity) FROM order_items oi JOIN orders o ON o.id = oi.order_id WHERE oi.product_id = p.id AND o.status <> 'canceled'), 0),
       p.stock + COALESCE((SELECT SUM(oi.quantity) FROM order_items oi JOIN orders o ON o.id = oi.order_id WHERE oi.product_id = p.id AND o.status = 'pending'), 0),
       COALESCE((SELECT SUM(oi.quantity) FROM order_items oi JOIN orders o ON o.id = oi.order_id WHERE oi.product_id = p.id AND o.status = 'pending'), 0),
       p.unit, 0, 'Tồn đầu kỳ chuyển từ Product.stock; lô không có hạn sử dụng'
FROM products p;

INSERT INTO order_inventory_allocations (order_item_id, batch_id, quantity, status, exported_at)
SELECT oi.id, b.id, oi.quantity,
       CASE WHEN o.status = 'pending' THEN 'RESERVED' ELSE 'EXPORTED' END,
       CASE WHEN o.status = 'pending' THEN NULL ELSE COALESCE(o.updated_at, o.created_at) END
FROM order_items oi
JOIN orders o ON o.id = oi.order_id
JOIN inventory_batches b ON b.product_id = oi.product_id AND b.batch_code = CONCAT('LEGACY-', oi.product_id)
WHERE o.status <> 'canceled';

INSERT INTO inventory_transactions (product_id, batch_id, type, quantity, quantity_before, quantity_after, reserved_before, reserved_after, reason)
SELECT product_id, id, 'IMPORT', remaining_quantity, 0, remaining_quantity, 0, reserved_quantity,
       'Số dư kho chuyển từ Product.stock và các đơn đang mở trước khi có module kho'
FROM inventory_batches WHERE batch_code LIKE 'LEGACY-%';
