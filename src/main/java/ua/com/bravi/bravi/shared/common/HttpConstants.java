package ua.com.bravi.bravi.shared.common;

import java.util.List;
import java.util.stream.Stream;

public final class HttpConstants {

    public static final String REQUEST_ID_HEADER = "X-Correlation-Id";
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String USER_AGENT_HEADER = "User-Agent";

    /** Public id of the account in scope (resolved into AccountContext). Present on almost all requests. */
    public static final String ACCOUNT_ID_HEADER = "X-Account-Id";
    /** Public id of the store in scope (resolved into StoreContext); only on store-scoped requests. */
    public static final String STORE_ID_HEADER = "X-Store-Id";

    /** Service-to-service endpoints (Auth Service → Backend); no end-user context. */
    public static final String INTERNAL_PATHS = "/internal/**";

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

    /** Paths that must not trigger end-user resolution/provisioning (public + service-to-service). */
    public static final List<String> NON_USER_PATHS = Stream.concat(
            EXCLUDED_PATHS.stream(), Stream.of(INTERNAL_PATHS)).toList();

    private HttpConstants() {
    }
}
