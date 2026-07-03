package ua.com.bravi.bravi.seller.stores.api;

/** Store settings read/patch model. Null fields on a patch leave the stored value unchanged. */
public record StoreSettings(
        String defaultCurrency,
        String defaultLanguage,
        String defaultWeightUnit,
        String defaultDimensionUnit,
        String timezone
) {
}
