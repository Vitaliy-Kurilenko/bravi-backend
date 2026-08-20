package ua.com.bravi.bravi.seller.catalog.discounts.api;

import java.math.BigDecimal;

/**
 * A product a discount operation addresses. The caller resolves it and proves ownership, so this
 * module never reads the product table itself.
 */
public record DiscountTarget(Long productId, String productPublicId, BigDecimal price) {
}
