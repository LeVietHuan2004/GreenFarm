INSERT INTO role_permissions(role_id, permission_id, created_at, updated_at)
SELECT roles.id, permissions.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles
JOIN permissions ON permissions.name = 'manage_orders'
WHERE roles.name = 'staff'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions
      WHERE role_permissions.role_id = roles.id
        AND role_permissions.permission_id = permissions.id
  );
