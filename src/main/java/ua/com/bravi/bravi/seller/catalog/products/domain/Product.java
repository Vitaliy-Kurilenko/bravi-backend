package ua.com.bravi.bravi.seller.catalog.products.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record Product(
        Long id,
        String publicId,
        Long storeId,
        String categoryId,
        String manufacturerId,
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
        Instant createdAt,
        Instant updatedAt
) {
}
