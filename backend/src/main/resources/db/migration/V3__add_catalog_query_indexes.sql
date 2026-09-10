CREATE INDEX idx_products_catalog_filter
    ON products (status, category_id, price);

CREATE INDEX idx_products_name
    ON products (name);
