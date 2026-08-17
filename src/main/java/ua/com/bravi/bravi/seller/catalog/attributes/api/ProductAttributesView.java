package ua.com.bravi.bravi.seller.catalog.attributes.api;

import java.util.List;

/**
 * Everything the product attribute editor needs in one read: what the product's category offers and
 * what the product currently carries.
 */
public record ProductAttributesView(
        List<CategoryAttributeView> offered,
        List<ProductAttributeValueView> values
) {
}
