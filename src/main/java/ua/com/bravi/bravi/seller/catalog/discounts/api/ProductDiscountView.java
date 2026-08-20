package ua.com.bravi.bravi.seller.catalog.discounts.api;

import java.math.BigDecimal;

/** The discount in effect on a product and the price it yields; {@code discount} is always ACTIVE. */
public record ProductDiscountView(DiscountView discount, BigDecimal discountedPrice, BigDecimal savings) {
}
