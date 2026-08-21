package ua.com.bravi.bravi.seller.tags.domain;

import lombok.Getter;
import ua.com.bravi.bravi.seller.tags.exception.InvalidTagRequestException;

/**
 * Whitelist of tag sort fields. {@code property} is the entity property name used to build a
 * {@code Sort}, so an arbitrary client string never reaches the query.
 */
@Getter
public enum TagSortBy {
    NAME("name"),
    CREATED_AT("createdAt"),
    UPDATED_AT("updatedAt");

    private final String property;

    TagSortBy(String property) {
        this.property = property;
    }

    /** Parses an external token such as {@code created_at} into the enum, case-insensitively. */
    public static TagSortBy fromParam(String token) {
        for (TagSortBy value : values()) {
            if (value.name().equalsIgnoreCase(token)) {
                return value;
            }
        }
        throw new InvalidTagRequestException("sort_by", "Unknown sort field: " + token);
    }
}
