package ua.com.bravi.bravi.seller.catalog.discounts.domain;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A discount a product carries for a period. It stores the seller's intent (type and value), never the
 * resulting price: that is recomputed from the product's current price on every read, so changing the
 * price cannot silently desynchronise the storefront from the admin panel.
 *
 * <p>{@code label} names the promotion for the seller only and is never shown to a buyer.
 */
public record Discount(
        Long id,
        String publicId,
        Long productId,
        DiscountType type,
        BigDecimal value,
        Instant startsAt,
        Instant endsAt,
        String label,
        Instant createdAt,
        Instant updatedAt) {

    public DiscountPeriod period() {
        return new DiscountPeriod(startsAt, endsAt);
    }

    public DiscountStatus statusAt(Instant now) {
        return period().statusAt(now);
    }

    public boolean activeAt(Instant now) {
        return period().contains(now);
    }
}
