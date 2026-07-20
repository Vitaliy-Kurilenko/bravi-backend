package ua.com.bravi.bravi.seller.stores.api;

import java.time.ZoneId;
import java.util.Currency;
import java.util.Locale;

/**
 * Per-store defaults; the single source of truth for the store timezone and currency.
 * Null fields on a patch leave the stored value unchanged.
 * The unit fields carry WEIGHT_UNIT / DIMENSION_UNIT dictionary codes.
 */
public record StoreSettings(
        Currency defaultCurrency,
        Locale defaultLanguage,
        String defaultWeightUnit,
        String defaultDimensionUnit,
        ZoneId timezone
) {
}
