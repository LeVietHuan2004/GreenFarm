ALTER TABLE orders
    ADD COLUMN inventory_released_at TIMESTAMP NULL AFTER delivered_at;

UPDATE orders
SET inventory_released_at = COALESCE(updated_at, created_at, CURRENT_TIMESTAMP)
WHERE status = 'canceled';

CREATE INDEX idx_payments_expiration
    ON payments(payment_method, status, expires_at);

INSERT INTO permissions(name, created_at, updated_at)
SELECT 'manage_coupons', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE name = 'manage_coupons'
);

INSERT INTO role_permissions(role_id, permission_id, created_at, updated_at)
SELECT roles.id, permissions.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles
JOIN permissions ON permissions.name = 'manage_coupons'
WHERE roles.name = 'admin'
  AND NOT EXISTS (
      SELECT 1
      FROM role_permissions
      WHERE role_permissions.role_id = roles.id
        AND role_permissions.permission_id = permissions.id
  );
