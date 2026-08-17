package ua.com.bravi.bravi.seller.catalog.attributes.api;

import ua.com.bravi.bravi.seller.catalog.attributes.domain.AttributeSource;

/**
 * An attribute offered to products of a category, together with where the offer comes from.
 * {@code sourceCategory} is filled for inherited entries, telling the seller which ancestor to edit.
 */
public record CategoryAttributeView(
        AttributeView attribute,
        AttributeSource source,
        String sourceCategoryId,
        String sourceCategoryName,
        Integer sortOrder
) {
}
