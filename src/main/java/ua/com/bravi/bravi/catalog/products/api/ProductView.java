package ua.com.bravi.bravi.catalog.products.api;

import ua.com.bravi.bravi.catalog.products.domain.ProductStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ProductView(
        Long id,
        Long storeId,
        Long categoryId,
        Long manufacturerId,
        Long stockStatusId,
        String name,
        String sku,
        String code,
        String description,
        BigDecimal partnerPrice,
        BigDecimal recommendedPrice,
        Integer quantity,
        BigDecimal weight,
        BigDecimal width,
        BigDecimal height,
        BigDecimal length,
        ProductStatus status,
        Instant createdAt,
        Instant updatedAt,
        List<ProductImageView> images
) {
}
