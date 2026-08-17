package ua.com.bravi.bravi.seller.controller.dto.in;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record CategoryAttributeUpdateRequest(
        @NotNull @PositiveOrZero @JsonProperty("sort_order") Integer sortOrder
) {
}
