package ua.com.bravi.bravi.seller.controller.dto.out;

import com.fasterxml.jackson.annotation.JsonProperty;
import ua.com.bravi.bravi.seller.catalog.discounts.domain.DiscountType;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * The discount shaping a product's current price, carried inside the product payload.
 *
 * <p>Deliberately narrower than the schedule entry: it answers "what is the price and until when".
 * {@code label} is the seller's own name for the promotion, so a buyer-facing projection must drop it.
 */
public record ActiveDiscountResponse(
        @JsonProperty("public_id")
        String publicId,
        DiscountType type,
        BigDecimal value,
        String label,
        @JsonProperty("ends_at")
        Instant endsAt
) {
}
