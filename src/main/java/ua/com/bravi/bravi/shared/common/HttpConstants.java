package ua.com.bravi.bravi.shared.common;

import java.util.List;

public final class HttpConstants {

    public static final String REQUEST_ID_HEADER = "X-Correlation-Id";
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String USER_AGENT_HEADER = "User-Agent";

    public static final String REQUEST_ID_MDC_KEY = "requestId";

    public static final List<String> REQUIRED_HEADERS = List.of(
            REQUEST_ID_HEADER
    );

    public static final List<String> EXCLUDED_PATHS = List.of(
            "/actuator/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/error"
    );

    private HttpConstants() {
    }
}
