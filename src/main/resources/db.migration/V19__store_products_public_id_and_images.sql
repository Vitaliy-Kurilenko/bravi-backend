-- products → store_products (+ public_id, partner_price → price, drop recommended_price);
-- product_images → store_product_images. Image bytes now live in object storage (S3/MinIO),
-- served via public URL; the app only keeps the storage key.

ALTER TABLE products RENAME TO store_products;

ALTER TABLE store_products RENAME CONSTRAINT pk_products TO pk_store_products;
ALTER TABLE store_products RENAME CONSTRAINT fk_products_on_store TO fk_store_products_on_store;
ALTER TABLE store_products RENAME CONSTRAINT fk_products_on_category TO fk_store_products_on_category;
ALTER TABLE store_products RENAME CONSTRAINT fk_products_on_manufacturer TO fk_store_products_on_manufacturer;
ALTER TABLE store_products RENAME CONSTRAINT fk_products_on_stock_status TO fk_store_products_on_stock_status;
ALTER TABLE store_products RENAME CONSTRAINT uq_products_store_code TO uq_store_products_store_code;

ALTER INDEX uq_products_store_sku RENAME TO uq_store_products_store_sku;
ALTER INDEX idx_products_store_id RENAME TO idx_store_products_store_id;
ALTER INDEX idx_products_category_id RENAME TO idx_store_products_category_id;
ALTER INDEX idx_products_manufacturer_id RENAME TO idx_store_products_manufacturer_id;
ALTER INDEX idx_products_stock_status_id RENAME TO idx_store_products_stock_status_id;
ALTER INDEX idx_products_created_at RENAME TO idx_store_products_created_at;
ALTER INDEX idx_products_name_lower RENAME TO idx_store_products_name_lower;

ALTER TABLE store_products RENAME COLUMN partner_price TO price;
ALTER TABLE store_products DROP COLUMN recommended_price;

ALTER TABLE store_products ADD COLUMN public_id VARCHAR(255);
UPDATE store_products SET public_id = 'prd_' || substr(md5(random()::text || id::text), 1, 16);
ALTER TABLE store_products ALTER COLUMN public_id SET NOT NULL;
ALTER TABLE store_products ADD CONSTRAINT uc_store_products_public_id UNIQUE (public_id);


ALTER TABLE product_images RENAME TO store_product_images;

ALTER TABLE store_product_images RENAME CONSTRAINT pk_product_images TO pk_store_product_images;
ALTER TABLE store_product_images RENAME CONSTRAINT fk_product_images_on_product TO fk_store_product_images_on_product;

ALTER INDEX idx_product_images_product_id RENAME TO idx_store_product_images_product_id;
