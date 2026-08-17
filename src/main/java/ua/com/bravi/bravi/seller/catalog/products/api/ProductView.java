package ua.com.bravi.bravi.seller.catalog.products.api;

import ua.com.bravi.bravi.seller.catalog.attributes.api.ProductAttributeValueView;
import ua.com.bravi.bravi.seller.catalog.products.domain.ProductStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** {@code attributes} is filled when a single product is read; a product page leaves it empty. */
public record ProductView(
        Long id,
        String publicId,
        Long storeId,
        CatalogRefView category,
        CatalogRefView manufacturer,
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
        List<ProductImageView> images,
        List<ProductAttributeValueView> attributes
) {
}
