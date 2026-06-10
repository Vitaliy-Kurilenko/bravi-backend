package ua.com.bravi.bravi.shared.util;

import java.util.regex.Pattern;

public final class ValidationPatterns {

    public static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public static final Pattern URL = Pattern.compile("^https?://[^\\s]+$");

    public static final Pattern PHONE = Pattern.compile("^\\+?[0-9 \\-]{6,20}$");

    public static final Pattern TELEGRAM_USERNAME = Pattern.compile("^@[A-Za-z0-9_]{5,32}$");

    private ValidationPatterns() {
    }
}
