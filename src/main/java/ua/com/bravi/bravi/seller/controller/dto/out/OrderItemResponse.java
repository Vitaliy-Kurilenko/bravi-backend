package ua.com.bravi.bravi.seller.controller.dto.out;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderItemResponse(
        Long id,
        @JsonProperty("product_id")
        Long productId,
        String sku,
        String code,
        String name,
        Integer quantity,
        @JsonProperty("partner_price")
        BigDecimal partnerPrice,
        @JsonProperty("sale_price")
        BigDecimal salePrice,
        @JsonProperty("created_at")
        Instant createdAt,
        @JsonProperty("updated_at")
        Instant updatedAt
) {
}
