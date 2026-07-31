package ua.com.bravi.bravi.seller.catalog.manufacturers.domain;

import lombok.Getter;
import ua.com.bravi.bravi.seller.catalog.manufacturers.exception.InvalidManufacturerRequestException;

/**
 * Whitelist of manufacturer sort fields. {@code property} is the {@code ManufacturerEntity} property
 * name used to build a {@code Sort}, so an arbitrary client string never reaches the query.
 */
@Getter
public enum ManufacturerSortBy {
    ID("id"),
    NAME("name"),
    STATUS("status"),
    CREATED_AT("createdAt");

    private final String property;

    ManufacturerSortBy(String property) {
        this.property = property;
    }

    /** Parses an external token such as {@code created_at} into the enum, case-insensitively. */
    public static ManufacturerSortBy fromParam(String token) {
        for (ManufacturerSortBy value : values()) {
            if (value.name().equalsIgnoreCase(token)) {
                return value;
            }
        }
        throw new InvalidManufacturerRequestException("sort_by", "Unknown sort field: " + token);
    }
}
