package ua.com.bravi.bravi.shared.util;

import org.junit.jupiter.api.Test;
import ua.com.bravi.bravi.shared.common.LoggingConstants;

import static org.assertj.core.api.Assertions.assertThat;

class LogSanitizerTest {

    @Test
    void masksSensitiveJsonFields() {
        String json = """
                {"name":"Shop","email":"seller@example.com","password":"hunter2","country":"PT"}""";

        String sanitized = LogSanitizer.sanitize(json);

        assertThat(sanitized).doesNotContain("seller@example.com", "hunter2");
        assertThat(sanitized).contains("\"email\":\"***\"", "\"password\":\"***\"");
        assertThat(sanitized).contains("\"name\":\"Shop\"", "\"country\":\"PT\"");
    }

    @Test
    void masksSensitiveFieldsInRecordToString() {
        String toString = "StoreContact[type=EMAIL, value=owner@shop.com, firstName=Ivan]";

        String sanitized = LogSanitizer.sanitize(toString);

        assertThat(sanitized).doesNotContain("owner@shop.com", "Ivan");
        assertThat(sanitized).contains("type=EMAIL", "value=***", "firstName=***");
    }

    @Test
    void masksQueryStringParameters() {
        String sanitized = LogSanitizer.sanitize("page=0&email=a%40b.com&sort=name");

        assertThat(sanitized).doesNotContain("a%40b.com");
        assertThat(sanitized).contains("page=0", "email=***", "sort=name");
    }

    @Test
    void isCaseInsensitiveAndHandlesNullJsonValues() {
        String sanitized = LogSanitizer.sanitize("{\"Email\":null,\"TOKEN\":\"abc\"}");

        assertThat(sanitized).doesNotContain("abc");
        assertThat(sanitized).contains("\"Email\":***", "\"TOKEN\":\"***\"");
    }

    @Test
    void collapsesWhitespaceIntoSingleLine() {
        String sanitized = LogSanitizer.sanitize("{\n  \"name\" : \"Shop\"\n}");

        assertThat(sanitized).isEqualTo("{ \"name\" : \"Shop\" }");
    }

    @Test
    void truncatesOverlongPayload() {
        String sanitized = LogSanitizer.sanitize("x".repeat(LoggingConstants.MAX_PAYLOAD_CHARS + 500));

        assertThat(sanitized).hasSizeLessThan(LoggingConstants.MAX_PAYLOAD_CHARS + 40);
        assertThat(sanitized).endsWith("(+500 chars)");
    }

    @Test
    void masksBareEmailValuePassedWithoutFieldName() {
        assertThat(LogSanitizer.sanitize("onb@example.com")).isEqualTo("***");
        assertThat(LogSanitizer.sanitize("[a@b.com, c@d.org]")).isEqualTo("[***, ***]");
    }

    @Test
    void describesNullAndPlainValues() {
        assertThat(LogSanitizer.describe(null)).isEqualTo("null");
        assertThat(LogSanitizer.describe(42L)).isEqualTo("42");
        assertThat(LogSanitizer.sanitize(null)).isEmpty();
    }
}
