package ua.com.bravi.bravi.shared.common;

import java.util.List;

/**
 * Налаштування діагностичного логування payload'ів і викликів сервісів.
 * Це фіксовані технічні переліки, а не конфіг середовища (див. §9 CLAUDE.md).
 */
public final class LoggingConstants {

    /** HTTP-тіла запитів/відповідей. */
    public static final String PAYLOAD_LOGGER = "ua.com.bravi.bravi.payload";
    /** Аргументи та результати викликів між сервісами/модулями. */
    public static final String SERVICE_CALL_LOGGER = "ua.com.bravi.bravi.calls";

    /** Довші payload'и обрізаються — логи не повинні тягнути мегабайти. */
    public static final int MAX_PAYLOAD_CHARS = 2000;

    /** Скільки байтів тіла запиту взагалі буферизувати (обмежує пам'ять, а не лише лог). */
    public static final int MAX_CACHED_BODY_BYTES = 8 * 1024;

    public static final String MASK = "***";

    /**
     * Імена полів, значення яких маскуються в будь-якому payload'і чи аргументі.
     * Свідомо ширші за потрібне: краще замаскувати зайве, ніж злити PII у логи.
     * {@code value} — бо в ньому лежить контакт магазину (email/телефон).
     */
    public static final List<String> SENSITIVE_KEYS = List.of(
            "password", "secret", "token", "authorization", "credential", "signature",
            "accessKey", "access_key", "secretKey", "secret_key",
            "email", "contactEmail", "contact_email",
            "phone", "firstName", "first_name", "lastName", "last_name",
            "value"
    );

    /** Тіла цих типів не логуються (бінарні/великі). */
    public static final List<String> SKIPPED_CONTENT_TYPES = List.of(
            "multipart/", "image/", "video/", "audio/", "application/octet-stream", "application/pdf"
    );

    private LoggingConstants() {
    }
}
