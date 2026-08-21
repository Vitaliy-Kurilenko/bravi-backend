package ua.com.bravi.bravi.seller.tags;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
 * The tag dictionary is one set of endpoints for every kind of tag, so the permission cannot be
 * spelled out in the annotation: it follows the target of the request. Nothing in the compiler
 * checks that expression, which is what this test is for — an account allowed to manage products
 * must not reach the order vocabulary through the same routes.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TagPermissionsTest extends AbstractPostgresIT {

    private static final String SERVICE_TOKEN = "service";
    private static final String USER_TOKEN = "user";
    private static final UUID EXT_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final String ROLE_CODE = "TEST_PRODUCT_ONLY";

    @TestConfiguration
    static class StubJwtDecoderConfig {

        @Bean
        JwtDecoder jwtDecoder() {
            Jwt service = Jwt.withTokenValue(SERVICE_TOKEN).header("alg", "none")
                    .subject(UUID.fromString("98989898-9898-9898-9898-989898989898").toString())
                    .claim("resource_access", Map.of("backend-service", Map.of("roles", List.of("auth_service"))))
                    .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300)).build();
            Jwt user = Jwt.withTokenValue(USER_TOKEN).header("alg", "none")
                    .subject(EXT_ID.toString())
                    .claim("preferred_username", "permissions@example.com")
                    .claim("email", "permissions@example.com")
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

    private String accountId;
    private String storeId;

    @BeforeEach
    void setUp() {
        accountId = registerSeller();
        storeId = createStore();
        grantProductPermissionsOnly();
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM role_permissions WHERE role_id IN "
                + "(SELECT id FROM roles WHERE code = ?)", ROLE_CODE);
        jdbcTemplate.update("DELETE FROM membership_roles WHERE role_id IN "
                + "(SELECT id FROM roles WHERE code = ?)", ROLE_CODE);
        jdbcTemplate.update("DELETE FROM roles WHERE code = ?", ROLE_CODE);
        TestDatabaseCleaner.clean(jdbcTemplate);
    }

    @Test
    void productPermissionsOpenTheProductVocabulary() {
        assertThat(get("/sellers/tags/products").getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(post("/sellers/tags/products", """
                {"name":"Хіт"}""").getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void theSameRoutesStayShutForTheOrderVocabulary() {
        assertThat(get("/sellers/tags/orders").getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(post("/sellers/tags/orders", """
                {"name":"Терміново"}""").getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void writingIsRefusedEvenWhereReadingIsAllowed() {
        jdbcTemplate.update("DELETE FROM role_permissions WHERE role_id IN "
                + "(SELECT id FROM roles WHERE code = ?) AND permission_id IN "
                + "(SELECT id FROM permissions WHERE code = 'PRODUCT_WRITE')", ROLE_CODE);

        assertThat(get("/sellers/tags/products").getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(post("/sellers/tags/products", """
                {"name":"Хіт"}""").getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    /** Assigning tags is guarded by a fixed resource, and must agree with the dictionary's answer. */
    @Test
    void theAssignmentRoutesFollowTheSameProductPermission() {
        assertThat(get("/sellers/product-tags/bulk").getStatusCode())
                .isNotEqualTo(HttpStatus.FORBIDDEN);
        assertThat(post("/sellers/product-tags/bulk", """
                {"product_ids":["prd_none"],"tags":[{"name":"Хіт"}]}""").getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    // --- helpers -------------------------------------------------------------------------------

    /**
     * Swaps the owner role for a custom one carrying store and product permissions only, so the
     * account can run a shop but has no business with orders.
     */
    private void grantProductPermissionsOnly() {
        Long internalAccountId = jdbcTemplate.queryForObject(
                "SELECT id FROM accounts WHERE public_id = ?", Long.class, accountId);
        jdbcTemplate.update("INSERT INTO roles (public_id, account_id, code, name, account_type, "
                        + "is_system, status, created_at) VALUES (?, ?, ?, 'Product only', 'SELLER', false, "
                        + "'ACTIVE', now())",
                UUID.randomUUID().toString(), internalAccountId, ROLE_CODE);
        Long roleId = jdbcTemplate.queryForObject(
                "SELECT id FROM roles WHERE code = ?", Long.class, ROLE_CODE);
        jdbcTemplate.update("INSERT INTO role_permissions (role_id, permission_id, created_at) "
                + "SELECT ?, p.id, now() FROM permissions p "
                + "WHERE p.account_type = 'SELLER' AND p.resource IN ('STORE', 'PRODUCT')", roleId);

        Long membershipId = jdbcTemplate.queryForObject(
                "SELECT m.id FROM memberships m JOIN users u ON u.id = m.user_id WHERE u.ext_id = ?",
                Long.class, EXT_ID);
        jdbcTemplate.update("DELETE FROM membership_roles WHERE membership_id = ?", membershipId);
        jdbcTemplate.update("INSERT INTO membership_roles (membership_id, role_id, assigned_at) "
                + "VALUES (?, ?, now())", membershipId, roleId);
    }

    private String registerSeller() {
        String body = "{\"keycloakUserId\":\"" + EXT_ID + "\",\"email\":\"permissions@example.com\","
                + "\"firstName\":\"First\",\"lastName\":\"Last\"}";
        assertThat(rest.exchange(url("/internal/registrations/seller"), HttpMethod.POST,
                new HttpEntity<>(body, headers(SERVICE_TOKEN, null, null)), String.class)
                .getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return jdbcTemplate.queryForObject(
                "SELECT a.public_id FROM accounts a JOIN memberships m ON m.account_id = a.id "
                        + "JOIN users u ON u.id = m.user_id WHERE u.ext_id = ?", String.class, EXT_ID);
    }

    private String createStore() {
        assertThat(rest.exchange(url("/sellers/onboarding/store"), HttpMethod.POST,
                new HttpEntity<>("{\"name\":\"Shop\"}", headers(USER_TOKEN, accountId, null)), String.class)
                .getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return jdbcTemplate.queryForObject("SELECT public_id FROM stores ORDER BY id DESC LIMIT 1", String.class);
    }

    private ResponseEntity<String> get(String path) {
        return rest.exchange(url(path), HttpMethod.GET, new HttpEntity<>(headers()), String.class);
    }

    private ResponseEntity<String> post(String path, String body) {
        return rest.exchange(url(path), HttpMethod.POST, new HttpEntity<>(body, headers()), String.class);
    }

    private HttpHeaders headers() {
        return headers(USER_TOKEN, accountId, storeId);
    }

    private static HttpHeaders headers(String token, String accountId, String storeId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HttpConstants.REQUEST_ID_HEADER, "corr-tag-permissions");
        if (accountId != null) {
            headers.add(HttpConstants.ACCOUNT_ID_HEADER, accountId);
        }
        if (storeId != null) {
            headers.add(HttpConstants.STORE_ID_HEADER, storeId);
        }
        return headers;
    }

    private String url(String path) {
        return "http://localhost:" + port + "/api" + path;
    }

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
