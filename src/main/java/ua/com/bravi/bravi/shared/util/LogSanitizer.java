package ua.com.bravi.bravi.shared.util;

import ua.com.bravi.bravi.shared.common.LoggingConstants;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Готує довільний payload (JSON-тіло, query-рядок, {@code toString()} аргумента) до логування:
 * маскує значення чутливих полів, збирає в один рядок і обрізає надто довге.
 * Використовується і фільтром payload'ів, і аспектом викликів сервісів.
 */
public final class LogSanitizer {

    private static final String KEYS = String.join("|", LoggingConstants.SENSITIVE_KEYS);

    /** JSON: {@code "email": "a@b.c"} або {@code "email":null}. */
    private static final Pattern JSON_FIELD = Pattern.compile(
            "(\"(?:" + KEYS + ")\"\\s*:\\s*)(\"(?:\\\\.|[^\"\\\\])*\"|[^,}\\]\\s]+)",
            Pattern.CASE_INSENSITIVE);

    /** record/toString та query: {@code email=a@b.c}, {@code email:a@b.c}. */
    private static final Pattern KEY_VALUE = Pattern.compile(
            "\\b((?:" + KEYS + ")\\s*[=:]\\s*)([^,)\\]}&\\s]*)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    /**
     * Страховка: значення, що виглядає як email, маскується навіть якщо воно передане
     * без імені поля (позиційний аргумент, елемент колекції тощо).
     */
    private static final Pattern EMAIL_VALUE = Pattern.compile(
            "[\\w.+-]+@[\\w-]+\\.[\\w.-]+", Pattern.CASE_INSENSITIVE);

    public static String sanitize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String collapsed = WHITESPACE.matcher(raw).replaceAll(" ").trim();
        String masked = mask(JSON_FIELD, collapsed);
        masked = mask(KEY_VALUE, masked);
        masked = EMAIL_VALUE.matcher(masked).replaceAll(LoggingConstants.MASK);
        return truncate(masked);
    }

    /** Готує аргумент/результат виклику сервісу; {@code null} лишається видимим як "null". */
    public static String describe(Object value) {
        return value == null ? "null" : sanitize(String.valueOf(value));
    }

    private static String mask(Pattern pattern, String input) {
        Matcher matcher = pattern.matcher(input);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String quoted = matcher.group(2).startsWith("\"") ? "\"" + LoggingConstants.MASK + "\"" : LoggingConstants.MASK;
            matcher.appendReplacement(out, Matcher.quoteReplacement(matcher.group(1) + quoted));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private static String truncate(String input) {
        if (input.length() <= LoggingConstants.MAX_PAYLOAD_CHARS) {
            return input;
        }
        int cut = input.length() - LoggingConstants.MAX_PAYLOAD_CHARS;
        return input.substring(0, LoggingConstants.MAX_PAYLOAD_CHARS) + "…(+" + cut + " chars)";
    }

    private LogSanitizer() {
    }
}
