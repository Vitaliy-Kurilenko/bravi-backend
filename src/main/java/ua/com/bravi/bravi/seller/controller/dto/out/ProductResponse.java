package ua.com.bravi.bravi.seller.controller.dto.out;

import com.fasterxml.jackson.annotation.JsonProperty;
import ua.com.bravi.bravi.seller.catalog.products.domain.ProductStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ProductResponse(
        @JsonProperty("public_id")
        String publicId,
        CatalogRefResponse category,
        CatalogRefResponse manufacturer,
        @JsonProperty("stock_status_id")
        Long stockStatusId,
        String name,
        String sku,
        String code,
        String description,
        BigDecimal price,
        Integer quantity,
        BigDecimal weight,
        BigDecimal width,
        BigDecimal height,
        BigDecimal length,
        ProductStatus status,
        List<ProductImageResponse> images,
        List<ProductAttributeValueResponse> attributes,
        @JsonProperty("discounted_price")
        BigDecimal discountedPrice,
        @JsonProperty("active_discount")
        ActiveDiscountResponse activeDiscount,
        @JsonProperty("created_at")
        Instant createdAt,
        @JsonProperty("updated_at")
        Instant updatedAt
) {
}
