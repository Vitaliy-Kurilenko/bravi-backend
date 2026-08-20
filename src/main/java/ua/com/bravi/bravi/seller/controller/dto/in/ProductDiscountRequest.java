package ua.com.bravi.bravi.seller.controller.dto.in;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import ua.com.bravi.bravi.seller.catalog.discounts.domain.DiscountType;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One entry of a submitted schedule. A {@code public_id} keeps the discount that already carries it;
 * without one the entry is created. A null {@code ends_at} makes the discount open-ended.
 *
 * <p>Read-only fields of the response are ignored rather than rejected, so a client may send a
 * schedule it just read straight back.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductDiscountRequest(
        @JsonProperty("public_id") String publicId,
        @NotNull DiscountType type,
        @NotNull @Positive BigDecimal value,
        @NotNull @JsonProperty("starts_at") Instant startsAt,
        @JsonProperty("ends_at") Instant endsAt,
        @Size(max = 255) String label
) {
}
