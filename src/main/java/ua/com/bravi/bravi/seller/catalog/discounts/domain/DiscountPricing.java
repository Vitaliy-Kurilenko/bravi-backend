package ua.com.bravi.bravi.seller.catalog.discounts.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * What a discount does to a price. The savings are rounded first and then subtracted, so the final
 * price and the savings always add back up to the original and both carry exactly two decimals.
 */
public record DiscountPricing(BigDecimal originalPrice, BigDecimal finalPrice, BigDecimal savings) {

    public static final int SCALE = 2;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    public static DiscountPricing of(BigDecimal price, Discount discount) {
        BigDecimal base = price.setScale(SCALE, ROUNDING);
        BigDecimal savings = discount.type() == DiscountType.PERCENT
                ? price.multiply(discount.value()).divide(HUNDRED, SCALE, ROUNDING)
                : discount.value().setScale(SCALE, ROUNDING);
        // Unreachable while the value rules hold; keeps a stale amount discount from pricing below zero.
        BigDecimal finalPrice = base.subtract(savings).max(BigDecimal.ZERO);
        return new DiscountPricing(base, finalPrice, base.subtract(finalPrice));
    }
}
