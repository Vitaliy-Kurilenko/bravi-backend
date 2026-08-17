package ua.com.bravi.bravi.seller.catalog.products.domain;

import ua.com.bravi.bravi.seller.catalog.attributes.domain.AttributeValue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** {@code attributes} carries the characteristic values submitted alongside the product itself. */
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
        Instant updatedAt,
        List<AttributeValue> attributes
) {
}
