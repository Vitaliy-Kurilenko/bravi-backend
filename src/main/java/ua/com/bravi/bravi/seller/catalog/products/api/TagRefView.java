package ua.com.bravi.bravi.seller.catalog.products.api;

/**
 * A tag as it rides along with its product: what the badge needs to be drawn, and nothing else
 * about the tag. Kept apart from {@link CatalogRefView} because a colour is meaningless on a
 * category or a manufacturer.
 */
public record TagRefView(
        String id,
        String name,
        String color
) {
}
