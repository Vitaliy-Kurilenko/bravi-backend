-- Store logo is now uploaded into our object storage (presigned URL flow).
-- logo_key is the storage object key (source of truth for delete/replace);
-- logo_url now holds the derived public URL of that object.
ALTER TABLE stores
    ADD COLUMN logo_key VARCHAR(512);
