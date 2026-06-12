package ua.com.bravi.bravi.seller.controller.dto.out;

import com.fasterxml.jackson.annotation.JsonProperty;
import ua.com.bravi.bravi.catalog.products.domain.ProductStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ProductResponse(
        Long id,
        @JsonProperty("category_id")
        Long categoryId,
        @JsonProperty("manufacturer_id")
        Long manufacturerId,
        @JsonProperty("stock_status_id")
        Long stockStatusId,
        String name,
        String sku,
        String code,
        String description,
        @JsonProperty("partner_price")
        BigDecimal partnerPrice,
        @JsonProperty("recommended_price")
        BigDecimal recommendedPrice,
        Integer quantity,
        BigDecimal weight,
        BigDecimal width,
        BigDecimal height,
        BigDecimal length,
        ProductStatus status,
        List<ProductImageResponse> images,
        @JsonProperty("created_at")
        Instant createdAt,
        @JsonProperty("updated_at")
        Instant updatedAt
) {
}
