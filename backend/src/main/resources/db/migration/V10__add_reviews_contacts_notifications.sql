DELETE older_review
FROM reviews older_review
JOIN reviews newer_review
  ON newer_review.user_id = older_review.user_id
 AND newer_review.product_id = older_review.product_id
 AND newer_review.id > older_review.id;

ALTER TABLE reviews
    MODIFY COLUMN comment VARCHAR(1000) NULL,
    ADD CONSTRAINT uq_reviews_user_product UNIQUE (user_id, product_id),
    ADD CONSTRAINT chk_reviews_rating CHECK (rating BETWEEN 1 AND 5);

ALTER TABLE contacts
    MODIFY COLUMN phone_number VARCHAR(30) NULL,
    MODIFY COLUMN message TEXT NOT NULL,
    ADD COLUMN user_id BIGINT UNSIGNED NULL AFTER id,
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'open' AFTER is_replied,
    ADD COLUMN response TEXT NULL AFTER status,
    ADD COLUMN responded_by BIGINT UNSIGNED NULL AFTER response,
    ADD COLUMN responded_at TIMESTAMP NULL AFTER responded_by,
    ADD INDEX idx_contacts_status_created (status, created_at),
    ADD CONSTRAINT contacts_user_id_foreign FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    ADD CONSTRAINT contacts_responded_by_foreign FOREIGN KEY (responded_by) REFERENCES users(id) ON DELETE SET NULL;

UPDATE contacts SET status = CASE WHEN is_replied = 1 THEN 'resolved' ELSE 'open' END;

DELETE FROM notifications WHERE user_id IS NULL;

ALTER TABLE notifications
    MODIFY COLUMN user_id BIGINT UNSIGNED NOT NULL,
    ADD INDEX idx_notifications_user_read_created (user_id, is_read, created_at);

INSERT INTO permissions(name, created_at, updated_at)
SELECT 'manage_contacts', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'manage_contacts');

INSERT INTO role_permissions(role_id, permission_id, created_at, updated_at)
SELECT roles.id, permissions.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles
JOIN permissions ON permissions.name = 'manage_contacts'
WHERE roles.name IN ('admin', 'staff')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions
      WHERE role_permissions.role_id = roles.id
        AND role_permissions.permission_id = permissions.id
  );
