package ua.com.bravi.bravi.seller.controller.dto.in;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.PositiveOrZero;

public record AttributeOptionUpdateRequest(
        String name,
        @PositiveOrZero @JsonProperty("sort_order") Integer sortOrder
) {
}
