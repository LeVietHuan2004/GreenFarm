ALTER TABLE users
    ADD COLUMN failed_login_attempts INT NOT NULL DEFAULT 0 AFTER google_id,
    ADD COLUMN locked_until TIMESTAMP NULL DEFAULT NULL AFTER failed_login_attempts,
    ADD COLUMN last_login_at TIMESTAMP NULL DEFAULT NULL AFTER locked_until;

ALTER TABLE role_permissions
    ADD CONSTRAINT uq_role_permissions UNIQUE (role_id, permission_id);
