-- allow_return and working_hours are per-store policy, so they move to store_settings
-- alongside the other configurable defaults; stores keeps only identity and address.

ALTER TABLE store_settings
    ADD COLUMN allow_return  BOOLEAN,
    ADD COLUMN working_hours JSONB;

-- Unlike V15 these columns have no counterpart in store_settings yet, so without a
-- backfill every store would silently lose its configured hours and return policy.
UPDATE store_settings ss
SET allow_return  = s.allow_return,
    working_hours = s.working_hours
FROM stores s
WHERE ss.store_id = s.id;

UPDATE store_settings
SET allow_return = false
WHERE allow_return IS NULL;

ALTER TABLE store_settings
    ALTER COLUMN allow_return SET NOT NULL;

ALTER TABLE stores
    DROP COLUMN allow_return,
    DROP COLUMN working_hours;
