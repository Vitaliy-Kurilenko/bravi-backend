package ua.com.bravi.bravi.seller.controller.dto.in;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.PositiveOrZero;
import ua.com.bravi.bravi.seller.catalog.products.domain.ProductStatus;

import java.math.BigDecimal;

public record ProductUpdateRequest(
        String name,
        String sku,
        String code,
        String description,
        @JsonProperty("category_id")
        Long categoryId,
        @JsonProperty("manufacturer_id")
        Long manufacturerId,
        @JsonProperty("stock_status_id")
        Long stockStatusId,
        @JsonProperty("partner_price")
        @PositiveOrZero
        BigDecimal partnerPrice,
        @JsonProperty("recommended_price")
        @PositiveOrZero
        BigDecimal recommendedPrice,
        @PositiveOrZero
        Integer quantity,
        BigDecimal weight,
        BigDecimal width,
        BigDecimal height,
        BigDecimal length,
        ProductStatus status
) {
}
