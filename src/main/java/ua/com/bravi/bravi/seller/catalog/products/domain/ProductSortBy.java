package ua.com.bravi.bravi.seller.catalog.products.domain;

import lombok.Getter;
import ua.com.bravi.bravi.seller.catalog.products.exception.InvalidProductRequestException;

/**
 * Whitelist of product sort fields. {@code property} is the {@code ProductEntity} property name
 * used to build a {@code Sort}, so an arbitrary client string never reaches the query.
 */
@Getter
public enum ProductSortBy {
    ID("id"),
    NAME("name"),
    PRICE("price"),
    QUANTITY("quantity"),
    STOCK_STATUS_ID("stockStatusId"),
    CREATED_AT("createdAt"),
    UPDATED_AT("updatedAt"),
    MANUFACTURER_ID("manufacturerId");

    private final String property;

    ProductSortBy(String property) {
        this.property = property;
    }

    /** Parses an external token such as {@code created_at} into the enum, case-insensitively. */
    public static ProductSortBy fromParam(String token) {
        for (ProductSortBy value : values()) {
            if (value.name().equalsIgnoreCase(token)) {
                return value;
            }
        }
        throw new InvalidProductRequestException("sort_by", "Unknown sort field: " + token);
    }
}
