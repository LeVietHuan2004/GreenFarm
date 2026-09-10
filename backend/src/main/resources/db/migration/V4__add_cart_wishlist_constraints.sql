ALTER TABLE cart_items
    ADD CONSTRAINT uk_cart_items_user_product UNIQUE (user_id, product_id),
    ADD CONSTRAINT chk_cart_items_quantity_positive CHECK (quantity > 0);

ALTER TABLE wishlists
    ADD CONSTRAINT uk_wishlists_user_product UNIQUE (user_id, product_id);
