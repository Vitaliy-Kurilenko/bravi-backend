package ua.com.bravi.bravi.seller.controller.dto.out;

import com.fasterxml.jackson.annotation.JsonProperty;
import ua.com.bravi.bravi.seller.catalog.discounts.domain.SkipReason;

/** A product a bulk application left untouched, and why. */
public record SkippedProductResponse(
        @JsonProperty("product_id")
        String productId,
        SkipReason reason,
        @JsonProperty("conflicting_discount_id")
        String conflictingDiscountId
) {
}
