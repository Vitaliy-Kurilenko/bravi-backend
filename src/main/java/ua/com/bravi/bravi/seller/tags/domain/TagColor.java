package ua.com.bravi.bravi.seller.tags.domain;

import ua.com.bravi.bravi.seller.tags.exception.InvalidTagRequestException;
import ua.com.bravi.bravi.shared.util.ValidationPatterns;

import java.util.Locale;

/**
 * The colour a tag's badge is drawn in. Colour pickers emit whatever shape they like, so the value
 * is brought to one canonical form before it is stored: the database check constraint describes
 * exactly that form, and this class is the only thing standing between the two.
 */
public final class TagColor {

    /** Characters in the canonical form, and the width of the column holding it. */
    public static final int LENGTH = 7;

    private static final int SHORTHAND_LENGTH = 4;

    private TagColor() {
    }

    /** Trims, expands the {@code #RGB} shorthand and upper-cases the result. */
    public static String normalize(String raw, String field) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidTagRequestException(field, "Tag colour is required");
        }
        String candidate = raw.strip().toUpperCase(Locale.ROOT);
        if (candidate.length() == SHORTHAND_LENGTH) {
            candidate = expand(candidate);
        }
        if (!ValidationPatterns.HEX_COLOR.matcher(candidate).matches()) {
            throw new InvalidTagRequestException(field,
                    "Tag colour must be a hex value such as #E5484D");
        }
        return candidate;
    }

    /** {@code #F80} means the same colour as {@code #FF8800}; only the long form is stored. */
    private static String expand(String shorthand) {
        StringBuilder expanded = new StringBuilder(LENGTH).append('#');
        for (int index = 1; index < shorthand.length(); index++) {
            expanded.append(shorthand.charAt(index)).append(shorthand.charAt(index));
        }
        return expanded.toString();
    }
}
