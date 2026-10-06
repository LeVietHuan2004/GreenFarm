CREATE TABLE refresh_tokens (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    token_hash CHAR(64) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP NULL,
    replaced_by_hash CHAR(64) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_refresh_tokens_hash (token_hash),
    KEY idx_refresh_tokens_user_active (user_id, revoked_at, expires_at),
    CONSTRAINT refresh_tokens_user_fk FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

ALTER TABLE payments
    ADD COLUMN invoice_email_attempts INT UNSIGNED NOT NULL DEFAULT 0 AFTER invoice_email_sent_at,
    ADD COLUMN invoice_email_next_retry_at TIMESTAMP NULL AFTER invoice_email_attempts,
    ADD COLUMN invoice_email_last_error VARCHAR(500) NULL AFTER invoice_email_next_retry_at;
