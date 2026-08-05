-- Position 0 is the product's main image, so the flag is derived and no longer stored.

ALTER TABLE store_product_images DROP COLUMN is_primary;

-- Deferred so a reorder may pass through transient duplicates inside one transaction.
ALTER TABLE store_product_images
    ADD CONSTRAINT uq_store_product_images_product_sort UNIQUE (product_id, sort_order)
        DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE store_product_images
    ADD CONSTRAINT ck_store_product_images_sort_order CHECK (sort_order >= 0);
