package ua.com.bravi.bravi.seller.controller.dto.out;

import com.fasterxml.jackson.annotation.JsonProperty;
import ua.com.bravi.bravi.seller.catalog.discounts.domain.DiscountStatus;
import ua.com.bravi.bravi.seller.catalog.discounts.domain.DiscountType;

import java.math.BigDecimal;
import java.time.Instant;

/** One discount of a product. {@code status} is derived from the period and is read-only. */
public record ProductDiscountResponse(
        @JsonProperty("public_id")
        String publicId,
        DiscountType type,
        BigDecimal value,
        @JsonProperty("starts_at")
        Instant startsAt,
        @JsonProperty("ends_at")
        Instant endsAt,
        String label,
        DiscountStatus status,
        @JsonProperty("created_at")
        Instant createdAt,
        @JsonProperty("updated_at")
        Instant updatedAt
) {
}
