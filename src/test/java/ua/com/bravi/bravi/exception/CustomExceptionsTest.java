package ua.com.bravi.bravi.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CustomExceptionsTest {

    @Test
    void missingRequiredHeaderCarriesHeaderName() {
        MissingRequiredHeaderException ex = new MissingRequiredHeaderException("X-Correlation-Id");

        assertThat(ex.getHeaderName()).isEqualTo("X-Correlation-Id");
        assertThat(ex.getMessage()).contains("X-Correlation-Id");
    }

    @Test
    void expiredJwtPreservesMessageAndCause() {
        Throwable cause = new RuntimeException("root");
        ExpiredJwtException ex = new ExpiredJwtException("expired", cause);

        assertThat(ex.getMessage()).isEqualTo("expired");
        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    void invalidJwtPreservesMessageAndCause() {
        Throwable cause = new RuntimeException("root");
        InvalidJwtException ex = new InvalidJwtException("invalid", cause);

        assertThat(ex.getMessage()).isEqualTo("invalid");
        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    void externalServicePreservesMessageAndCause() {
        Throwable cause = new RuntimeException("downstream");
        ExternalServiceException ex = new ExternalServiceException("keycloak failed", cause);

        assertThat(ex.getMessage()).isEqualTo("keycloak failed");
        assertThat(ex.getCause()).isSameAs(cause);
    }
}
