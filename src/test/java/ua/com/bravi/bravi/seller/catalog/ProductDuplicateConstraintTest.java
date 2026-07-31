package ua.com.bravi.bravi.seller.catalog;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import ua.com.bravi.bravi.AbstractPostgresIT;
import ua.com.bravi.bravi.TestDatabaseCleaner;
import ua.com.bravi.bravi.shared.common.HttpConstants;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies against a real database that a duplicate product is reported as a 409 naming the
 * offending field: {@code code} is guarded by a table constraint, {@code sku} by a partial unique index.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductDuplicateConstraintTest extends AbstractPostgresIT {

    private static final String SERVICE_TOKEN = "service";
    private static final String USER_TOKEN = "user";
    private static final UUID EXT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final String STORE_BODY = "{\"name\":\"Shop\"}";

    @TestConfiguration
    static class StubJwtDecoderConfig {

        @Bean
        JwtDecoder jwtDecoder() {
            Jwt service = Jwt.withTokenValue(SERVICE_TOKEN).header("alg", "none")
                    .subject(UUID.fromString("99999999-9999-9999-9999-999999999999").toString())
                    .claim("resource_access", Map.of("backend-service", Map.of("roles", List.of("auth_service"))))
                    .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300)).build();
            Jwt user = Jwt.withTokenValue(USER_TOKEN).header("alg", "none")
                    .subject(EXT_ID.toString())
                    .claim("preferred_username", "seller@example.com")
                    .claim("email", "seller@example.com")
                    .claim("given_name", "First")
                    .claim("family_name", "Last")
                    .claim("resource_access", Map.of("backend-service", Map.of("roles", List.of("bravi_user"))))
                    .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300)).build();
            return token -> SERVICE_TOKEN.equals(token) ? service : user;
        }
    }

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RestTemplate rest = restTemplateThatDoesNotThrow();

    @AfterEach
    void cleanUp() {
        TestDatabaseCleaner.clean(jdbcTemplate);
    }

    @Test
    void duplicateCodeAndDuplicateSkuAreReportedOnTheirOwnFields() {
        String accountId = registerSeller();
        String storeId = createStore(accountId);
        Long stockStatusId = jdbcTemplate.queryForObject(
                "SELECT id FROM stock_statuses ORDER BY id LIMIT 1", Long.class);

        assertThat(createProduct(accountId, storeId, "CODE-1", "SKU-1", stockStatusId).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);

        ResponseEntity<String> duplicateCode = createProduct(accountId, storeId, "CODE-1", "SKU-2", stockStatusId);
        assertThat(duplicateCode.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(duplicateCode.getBody()).contains("\"field\":\"code\"");

        ResponseEntity<String> duplicateSku = createProduct(accountId, storeId, "CODE-2", "SKU-1", stockStatusId);
        assertThat(duplicateSku.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(duplicateSku.getBody()).contains("\"field\":\"sku\"");

        assertThat(createProduct(accountId, storeId, "CODE-2", "SKU-2", stockStatusId).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
        String secondPublicId = jdbcTemplate.queryForObject(
                "SELECT public_id FROM store_products WHERE code = ?", String.class, "CODE-2");

        ResponseEntity<String> patchedIntoDuplicateSku = exchange(HttpMethod.PATCH,
                "/sellers/products/" + secondPublicId, "{\"sku\":\"SKU-1\"}", accountId, storeId);
        assertThat(patchedIntoDuplicateSku.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(patchedIntoDuplicateSku.getBody()).contains("\"field\":\"sku\"");
    }

    private String registerSeller() {
        String body = "{\"keycloakUserId\":\"" + EXT_ID + "\",\"email\":\"seller@example.com\","
                + "\"firstName\":\"First\",\"lastName\":\"Last\"}";
        ResponseEntity<String> response = rest.exchange(url("/internal/registrations/seller"), HttpMethod.POST,
                new HttpEntity<>(body, headers(SERVICE_TOKEN, null, null)), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return jdbcTemplate.queryForObject(
                "SELECT a.public_id FROM accounts a JOIN memberships m ON m.account_id = a.id "
                        + "JOIN users u ON u.id = m.user_id WHERE u.ext_id = ?", String.class, EXT_ID);
    }

    private String createStore(String accountId) {
        assertThat(exchange(HttpMethod.POST, "/sellers/onboarding/store", STORE_BODY, accountId, null)
                .getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return jdbcTemplate.queryForObject("SELECT public_id FROM stores ORDER BY id DESC LIMIT 1", String.class);
    }

    private ResponseEntity<String> createProduct(String accountId, String storeId,
                                                 String code, String sku, Long stockStatusId) {
        String body = "{\"name\":\"Widget\",\"code\":\"" + code + "\",\"sku\":\"" + sku + "\","
                + "\"stock_status_id\":" + stockStatusId + ",\"price\":10.00,\"quantity\":1}";
        return exchange(HttpMethod.POST, "/sellers/products", body, accountId, storeId);
    }

    private ResponseEntity<String> exchange(HttpMethod method, String path, String body,
                                            String accountId, String storeId) {
        return rest.exchange(url(path), method,
                new HttpEntity<>(body, headers(USER_TOKEN, accountId, storeId)), String.class);
    }

    private String url(String path) {
        return "http://localhost:" + port + "/api" + path;
    }

    private static HttpHeaders headers(String token, String accountId, String storeId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HttpConstants.REQUEST_ID_HEADER, "corr-duplicate");
        if (accountId != null) {
            headers.add(HttpConstants.ACCOUNT_ID_HEADER, accountId);
        }
        if (storeId != null) {
            headers.add(HttpConstants.STORE_ID_HEADER, storeId);
        }
        return headers;
    }

    /** Client that reports error statuses instead of throwing, and supports PATCH via the JDK HTTP client. */
    private static RestTemplate restTemplateThatDoesNotThrow() {
        RestTemplate template = new RestTemplate(new JdkClientHttpRequestFactory());
        template.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) {
                return false;
            }
        });
        return template;
    }
}
