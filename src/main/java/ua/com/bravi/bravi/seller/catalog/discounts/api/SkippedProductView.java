package ua.com.bravi.bravi.seller.catalog.discounts.api;

import ua.com.bravi.bravi.seller.catalog.discounts.domain.SkipReason;

/** A product a bulk application left alone, and why. */
public record SkippedProductView(String productPublicId, SkipReason reason, String conflictingDiscountPublicId) {
}
