package ua.com.bravi.bravi.seller.catalog.products.domain;

import ua.com.bravi.bravi.seller.catalog.attributes.domain.AttributeValue;
import ua.com.bravi.bravi.seller.tags.domain.TagRef;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * {@code attributes} and {@code tags} carry what was submitted alongside the product itself. On a
 * patch both tell absent from empty: null leaves the collection alone, an empty list clears it.
 */
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
        List<AttributeValue> attributes,
        List<TagRef> tags
) {
}
