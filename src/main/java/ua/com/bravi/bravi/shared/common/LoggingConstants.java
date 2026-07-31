package ua.com.bravi.bravi.shared.common;

import java.util.List;

/**
 * Settings of the diagnostic logging of payloads and service calls.
 * These are fixed technical listings rather than environment configuration.
 */
public final class LoggingConstants {

    /** Logger for HTTP request and response bodies. */
    public static final String PAYLOAD_LOGGER = "ua.com.bravi.bravi.payload";
    /** Logger for arguments and results of calls between services and modules. */
    public static final String SERVICE_CALL_LOGGER = "ua.com.bravi.bravi.calls";

    /** Payloads longer than this are truncated. */
    public static final int MAX_PAYLOAD_CHARS = 2000;

    /** Maximum number of request body bytes buffered in memory. */
    public static final int MAX_CACHED_BODY_BYTES = 8 * 1024;

    public static final String MASK = "***";

    /**
     * Names of fields whose values are masked in any payload or argument.
     * Deliberately broader than strictly needed; {@code value} is included because it carries
     * a store contact such as an email or a phone number.
     */
    public static final List<String> SENSITIVE_KEYS = List.of(
            "password", "secret", "token", "authorization", "credential", "signature",
            "accessKey", "access_key", "secretKey", "secret_key",
            "email", "contactEmail", "contact_email",
            "phone", "firstName", "first_name", "lastName", "last_name",
            "value"
    );

    /** Bodies of these content types are never logged. */
    public static final List<String> SKIPPED_CONTENT_TYPES = List.of(
            "multipart/", "image/", "video/", "audio/", "application/octet-stream", "application/pdf"
    );

    private LoggingConstants() {
    }
}
