package ua.com.bravi.bravi.seller.tags.domain;

import ua.com.bravi.bravi.seller.tags.exception.InvalidTagRequestException;

/** How a multi-tag filter combines its tags: carrying any of them, or all of them. */
public enum TagsMatch {
    ANY,
    ALL;

    public static TagsMatch fromParam(String token) {
        for (TagsMatch value : values()) {
            if (value.name().equalsIgnoreCase(token)) {
                return value;
            }
        }
        throw new InvalidTagRequestException("tags_match", "Unknown tags match mode: " + token);
    }
}
