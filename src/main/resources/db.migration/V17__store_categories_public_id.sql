ALTER TABLE categories RENAME TO store_categories;

ALTER TABLE store_categories RENAME CONSTRAINT pk_categories TO pk_store_categories;
ALTER TABLE store_categories RENAME CONSTRAINT fk_categories_on_store  TO fk_store_categories_on_store;
ALTER TABLE store_categories RENAME CONSTRAINT fk_categories_on_parent TO fk_store_categories_on_parent;

ALTER INDEX idx_categories_store_id   RENAME TO idx_store_categories_store_id;
ALTER INDEX idx_categories_parent_id  RENAME TO idx_store_categories_parent_id;
ALTER INDEX uq_categories_root_name   RENAME TO uq_store_categories_root_name;
ALTER INDEX uq_categories_child_name  RENAME TO uq_store_categories_child_name;

ALTER TABLE store_categories ADD COLUMN public_id VARCHAR(255);
UPDATE store_categories SET public_id = 'cat_' || substr(md5(random()::text || id::text), 1, 16);
ALTER TABLE store_categories ALTER COLUMN public_id SET NOT NULL;
ALTER TABLE store_categories ADD CONSTRAINT uc_store_categories_public_id UNIQUE (public_id);
