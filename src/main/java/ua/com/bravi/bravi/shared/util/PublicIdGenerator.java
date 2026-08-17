package ua.com.bravi.bravi.shared.util;

import java.security.SecureRandom;

/**
 * Generates opaque, URL-safe public identifiers with a type prefix (e.g. {@code usr_K9mP2xQa7LwZ8tBn}).
 * Public ids are what the API exposes; internal bigint ids never leave a module.
 */
public final class PublicIdGenerator {

    public static final String USER_PREFIX = "usr";
    public static final String ACCOUNT_PREFIX = "acc";
    public static final String MEMBERSHIP_PREFIX = "mem";
    public static final String STORE_PREFIX = "st";
    public static final String CATEGORY_PREFIX = "cat";
    public static final String MANUFACTURER_PREFIX = "mfr";
    public static final String PRODUCT_PREFIX = "prd";
    public static final String ATTRIBUTE_PREFIX = "attr";
    public static final String ATTRIBUTE_OPTION_PREFIX = "aopt";

    private static final char[] ALPHABET =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
    private static final int TOKEN_LENGTH = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PublicIdGenerator() {
    }

    public static String userId() {
        return generate(USER_PREFIX);
    }

    public static String generate(String prefix) {
        StringBuilder sb = new StringBuilder(prefix.length() + 1 + TOKEN_LENGTH);
        sb.append(prefix).append('_');
        for (int i = 0; i < TOKEN_LENGTH; i++) {
            sb.append(ALPHABET[RANDOM.nextInt(ALPHABET.length)]);
        }
        return sb.toString();
    }
}
