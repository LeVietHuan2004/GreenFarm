CREATE TABLE IF NOT EXISTS chat_messages (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NULL,
    guest_token VARCHAR(100) NULL,
    sender ENUM('user', 'bot') NOT NULL DEFAULT 'user',
    message TEXT NOT NULL,
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_chat_messages_user_created (user_id, created_at),
    KEY idx_chat_messages_guest_token (guest_token),
    CONSTRAINT fk_chat_messages_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

ALTER TABLE chat_messages ADD COLUMN guest_token_hash CHAR(64) NULL AFTER guest_token;
UPDATE chat_messages
SET guest_token_hash = SHA2(guest_token, 256), guest_token = NULL
WHERE guest_token IS NOT NULL AND guest_token_hash IS NULL;
