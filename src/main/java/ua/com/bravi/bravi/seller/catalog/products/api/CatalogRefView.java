package ua.com.bravi.bravi.seller.catalog.products.api;

/** Reference to a neighbouring catalog aggregate such as a category or a manufacturer: public id and name. */
public record CatalogRefView(
        String id,
        String name
) {
}
