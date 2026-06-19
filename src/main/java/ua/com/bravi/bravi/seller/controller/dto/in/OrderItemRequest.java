package ua.com.bravi.bravi.seller.controller.dto.in;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record OrderItemRequest(
        @JsonProperty("product_id")
        @NotNull
        Long productId,
        @NotNull
        @Positive
        Integer quantity,
        @JsonProperty("sale_price")
        @PositiveOrZero
        BigDecimal salePrice
) {
}
