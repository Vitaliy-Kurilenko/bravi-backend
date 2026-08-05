package ua.com.bravi.bravi.seller.controller.dto.in;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** Body to move a product image inside the gallery; position 0 is the product's main image. */
public record ProductImageUpdateRequest(
        @NotNull
        @PositiveOrZero
        @JsonProperty("sort_order")
        Integer sortOrder
) {
}
