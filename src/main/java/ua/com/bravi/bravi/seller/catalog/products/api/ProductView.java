package ua.com.bravi.bravi.seller.catalog.products.api;

import ua.com.bravi.bravi.seller.catalog.attributes.api.ProductAttributeValueView;
import ua.com.bravi.bravi.seller.catalog.discounts.api.DiscountView;
import ua.com.bravi.bravi.seller.catalog.products.domain.ProductStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * {@code attributes} is filled when a single product is read; a product page leaves it empty.
 *
 * <p>{@code tags} is filled on a page too, unlike {@code attributes}: the listing is where the
 * badges are read and where a tag filter is confirmed, and one bounded query fills them.
 *
 * <p>{@code discountedPrice} and {@code activeDiscount} are both null unless a discount is in effect
 * right now. The price is computed here rather than stored, so it can never drift from the base price.
 */
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
        List<ProductAttributeValueView> attributes,
        List<TagRefView> tags,
        BigDecimal discountedPrice,
        DiscountView activeDiscount
) {
}
