ALTER TABLE manufacturers RENAME TO store_manufacturers;

ALTER TABLE store_manufacturers RENAME CONSTRAINT pk_manufacturers TO pk_store_manufacturers;
ALTER TABLE store_manufacturers RENAME CONSTRAINT uq_manufacturers_store_name TO uq_store_manufacturers_store_name;
ALTER TABLE store_manufacturers RENAME CONSTRAINT fk_manufacturers_on_store TO fk_store_manufacturers_on_store;

ALTER INDEX idx_manufacturers_store_id RENAME TO idx_store_manufacturers_store_id;

ALTER TABLE store_manufacturers ADD COLUMN public_id VARCHAR(255);
UPDATE store_manufacturers SET public_id = 'mfr_' || substr(md5(random()::text || id::text), 1, 16);
ALTER TABLE store_manufacturers ALTER COLUMN public_id SET NOT NULL;
ALTER TABLE store_manufacturers ADD CONSTRAINT uc_store_manufacturers_public_id UNIQUE (public_id);
