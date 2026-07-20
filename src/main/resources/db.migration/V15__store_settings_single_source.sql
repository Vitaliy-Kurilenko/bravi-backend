-- store_settings becomes the single source of truth for timezone and currency.
-- Both were duplicated in stores (NOT NULL there, nullable here) with nothing keeping
-- the copies in sync: /seller/stores PATCH wrote one, the onboarding settings PATCH the other.

-- Onboarding defaults for rows predating the settings row (see StoreService).
UPDATE store_settings
SET default_language       = COALESCE(default_language, 'en'),
    default_weight_unit    = COALESCE(default_weight_unit, 'KG'),
    default_dimension_unit = COALESCE(default_dimension_unit, 'CM');

ALTER TABLE store_settings
    ALTER COLUMN timezone SET NOT NULL,
    ALTER COLUMN default_currency SET NOT NULL,
    ALTER COLUMN default_language SET NOT NULL,
    ALTER COLUMN default_weight_unit SET NOT NULL,
    ALTER COLUMN default_dimension_unit SET NOT NULL;

ALTER TABLE stores
    DROP COLUMN timezone,
    DROP COLUMN currency;