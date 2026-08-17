package ua.com.bravi.bravi.shared.util;

import java.util.regex.Pattern;

public final class ValidationPatterns {

    public static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public static final Pattern URL = Pattern.compile("^https?://[^\\s]+$");

    public static final Pattern PHONE = Pattern.compile("^\\+?[0-9 \\-]{6,20}$");

    public static final Pattern TELEGRAM_USERNAME = Pattern.compile("^@[A-Za-z0-9_]{5,32}$");

    /** Stable machine-readable identifier a seller assigns, such as an attribute or option code. */
    public static final Pattern RESOURCE_CODE = Pattern.compile("^[A-Z][A-Z0-9_]{0,63}$");

    private ValidationPatterns() {
    }
}
