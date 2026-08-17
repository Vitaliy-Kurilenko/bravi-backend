package ua.com.bravi.bravi.seller.catalog.attributes.domain;

import lombok.Getter;
import ua.com.bravi.bravi.seller.catalog.attributes.exception.InvalidAttributeRequestException;

/**
 * Whitelist of attribute sort fields. {@code property} is the {@code AttributeEntity} property name
 * used to build a {@code Sort}, so an arbitrary client string never reaches the query.
 */
@Getter
public enum AttributeSortBy {

    ID("id"),
    CODE("code"),
    NAME("name"),
    VALUE_TYPE("valueType"),
    SCOPE("scope"),
    STATUS("status"),
    CREATED_AT("createdAt");

    private final String property;

    AttributeSortBy(String property) {
        this.property = property;
    }

    /** Parses an external token such as {@code created_at} into the enum, case-insensitively. */
    public static AttributeSortBy fromParam(String token) {
        for (AttributeSortBy value : values()) {
            if (value.name().equalsIgnoreCase(token)) {
                return value;
            }
        }
        throw new InvalidAttributeRequestException("sort_by", "Unknown sort field: " + token);
    }
}
