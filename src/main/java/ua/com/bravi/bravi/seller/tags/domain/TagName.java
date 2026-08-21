package ua.com.bravi.bravi.seller.tags.domain;

import ua.com.bravi.bravi.seller.tags.exception.InvalidTagRequestException;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * The written form of a tag name and the key it deduplicates on. Names are typed by hand, so they
 * are normalized before they reach the database: the seller keeps the spelling he typed, while the
 * unique index sees the lower-cased normal form.
 */
public final class TagName {

    public static final int MAX_LENGTH = 64;

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private TagName() {
    }

    /** Trims, collapses internal whitespace runs into one space, and validates the result. */
    public static String normalize(String raw, String field) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidTagRequestException(field, "Tag name is required");
        }
        String normalized = WHITESPACE.matcher(raw.strip()).replaceAll(" ");
        if (normalized.length() > MAX_LENGTH) {
            throw new InvalidTagRequestException(field,
                    "Tag name must not exceed " + MAX_LENGTH + " characters");
        }
        return normalized;
    }

    /**
     * The dedup key, mirroring the {@code lower(name)} expression of the unique index. The database
     * stays the authority: where Java and PostgreSQL lower-case differently the key is merely more
     * permissive, which costs one insert the index then absorbs.
     */
    public static String key(String normalizedName) {
        return normalizedName.toLowerCase(Locale.ROOT);
    }

    /** Field name reported in validation errors, addressing the entry inside the submitted array. */
    public static String fieldOf(int index, String property) {
        return "tags[" + index + "]." + property;
    }
}
