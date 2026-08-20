package ua.com.bravi.bravi.seller.controller.dto.in;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import ua.com.bravi.bravi.seller.catalog.discounts.domain.DiscountType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** One discount to apply across many products of the store. */
public record ProductDiscountsBulkRequest(
        @NotEmpty @Size(max = 200) @JsonProperty("product_ids") List<String> productIds,
        @NotNull DiscountType type,
        @NotNull @Positive BigDecimal value,
        @NotNull @JsonProperty("starts_at") Instant startsAt,
        @JsonProperty("ends_at") Instant endsAt,
        @Size(max = 255) String label
) {
}
