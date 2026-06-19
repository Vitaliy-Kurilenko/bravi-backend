package ua.com.bravi.bravi.seller.controller.dto.in;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/** PATCH позиції замовлення: всі поля опційні (product_id → заміна товару, quantity, sale_price). */
public record OrderItemEditRequest(
        @JsonProperty("product_id")
        Long productId,
        @Positive
        Integer quantity,
        @JsonProperty("sale_price")
        @PositiveOrZero
        BigDecimal salePrice
) {
}
