ALTER TABLE products ADD COLUMN origin VARCHAR(255) NULL, ADD COLUMN certification VARCHAR(1000) NULL, ADD COLUMN storage_instructions TEXT NULL;
ALTER TABLE users ADD COLUMN credential_version INT NOT NULL DEFAULT 0;
CREATE TABLE greenfarm_password_reset_tokens (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT UNSIGNED NOT NULL,
  token_hash CHAR(64) NOT NULL,
  credential_version INT NOT NULL,
  created_at DATETIME NOT NULL,
  expires_at DATETIME NOT NULL,
  used_at DATETIME NULL,
  CONSTRAINT fk_greenfarm_reset_user FOREIGN KEY (user_id) REFERENCES users(id),
  UNIQUE KEY uk_greenfarm_reset_hash (token_hash),
  INDEX ix_greenfarm_reset_user_created (user_id, created_at)
) ENGINE=InnoDB;
