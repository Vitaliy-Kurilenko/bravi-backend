package ua.com.bravi.bravi.seller.catalog.discounts.api;

import ua.com.bravi.bravi.seller.catalog.discounts.domain.DiscountStatus;
import ua.com.bravi.bravi.seller.catalog.discounts.domain.DiscountType;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One discount of a product. {@code status} is resolved at read time from the period, never stored.
 *
 * <p>{@code label} names the promotion for the seller alone; a buyer-facing projection must drop it.
 */
public record DiscountView(
        String publicId,
        DiscountType type,
        BigDecimal value,
        Instant startsAt,
        Instant endsAt,
        String label,
        DiscountStatus status,
        Instant createdAt,
        Instant updatedAt) {
}
