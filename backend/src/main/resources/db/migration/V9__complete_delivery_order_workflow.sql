ALTER TABLE orders
    ADD COLUMN delivery_claimed_at TIMESTAMP NULL AFTER delivery_staff_id;

UPDATE order_status_history
SET status = 'pending'
WHERE status IS NULL;

ALTER TABLE order_status_history
    MODIFY COLUMN status ENUM(
        'pending',
        'processing',
        'ready_for_delivery',
        'out_for_delivery',
        'delivered',
        'delivery_failed',
        'completed',
        'canceled'
    ) NOT NULL;
