package ua.com.bravi.bravi.seller.catalog.discounts.domain;

/** Why a bulk application left a product untouched. */
public enum SkipReason {
    PERIOD_OVERLAP,
    AMOUNT_EXCEEDS_PRICE
}
