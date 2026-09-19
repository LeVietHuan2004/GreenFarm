ALTER TABLE payments
    ADD COLUMN invoice_email_sent_at TIMESTAMP NULL AFTER paid_at;
