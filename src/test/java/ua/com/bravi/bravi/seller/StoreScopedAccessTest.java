package ua.com.bravi.bravi.seller;

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
 * End-to-end DB-backed verification that the store is resolved from the URL path
 * ({@code /stores/{storePublicId}/...}), its account derived from it, and access scoped to the
 * caller's ACTIVE membership: a seller reaches only their own store; a store of another account,
 * or an unknown store, both yield 404 (a store you cannot see is "not found").
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StoreScopedAccessTest extends AbstractPostgresIT {

    private static final String SERVICE_TOKEN = "service";
    private static final UUID EXT_A = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID EXT_B = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String TOKEN_A = "userA";
    private static final String TOKEN_B = "userB";

    private static final String STORE_BODY = "{\"name\":\"Shop\"}";
    private static final String CATEGORY_BODY = "{\"name\":\"Shoes\"}";

    @TestConfiguration
    static class StubJwtDecoderConfig {

        @Bean
        JwtDecoder jwtDecoder() {
            Jwt service = Jwt.withTokenValue(SERVICE_TOKEN).header("alg", "none")
                    .subject(UUID.fromString("99999999-9999-9999-9999-999999999999").toString())
                    .claim("resource_access", Map.of("backend-service", Map.of("roles", List.of("auth_service"))))
                    .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300)).build();
            return token -> switch (token) {
                case SERVICE_TOKEN -> service;
                case TOKEN_B -> userToken(TOKEN_B, EXT_B, "b@example.com");
                default -> userToken(TOKEN_A, EXT_A, "a@example.com");
            };
        }

        private static Jwt userToken(String value, UUID extId, String email) {
            return Jwt.withTokenValue(value).header("alg", "none")
                    .subject(extId.toString())
                    .claim("preferred_username", email)
                    .claim("email", email)
                    .claim("given_name", "First")
                    .claim("family_name", "Last")
                    .claim("resource_access", Map.of("backend-service", Map.of("roles", List.of("bravi_user"))))
                    .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300)).build();
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
    void categoriesAreScopedToTheCallersOwnStore() {
        // Two independent sellers, each with their own account + store.
        registerAndCreateStore(EXT_A, "a@example.com", TOKEN_A);
        String storeA = latestStorePublicId();
        registerAndCreateStore(EXT_B, "b@example.com", TOKEN_B);
        String storeB = storePublicIdOtherThan(storeA);

        String ownCategories = "/stores/" + storeA + "/categories";

        // Owner reaches their own store: create + list.
        assertThat(post(ownCategories, CATEGORY_BODY, TOKEN_A).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(get(ownCategories, TOKEN_A).getStatusCode()).isEqualTo(HttpStatus.OK);

        // A store whose account the caller has no membership on → 404 (not yours = not found).
        assertThat(get("/stores/" + storeB + "/categories", TOKEN_A).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        // Unknown store → 404.
        assertThat(get("/stores/st_missing/categories", TOKEN_A).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    private String registerAndCreateStore(UUID extId, String email, String token) {
        String body = "{\"keycloakUserId\":\"" + extId + "\",\"email\":\"" + email + "\","
                + "\"firstName\":\"First\",\"lastName\":\"Last\"}";
        assertThat(post("/internal/registrations/seller", body, SERVICE_TOKEN).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
        String accountPublicId = jdbcTemplate.queryForObject(
                "SELECT a.public_id FROM accounts a JOIN memberships m ON m.account_id = a.id "
                        + "JOIN users u ON u.id = m.user_id WHERE u.ext_id = ?", String.class, extId);
        assertThat(post("/accounts/" + accountPublicId + "/seller/onboarding/store", STORE_BODY, token).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
        return accountPublicId;
    }

    private String latestStorePublicId() {
        return jdbcTemplate.queryForObject("SELECT public_id FROM stores ORDER BY id DESC LIMIT 1", String.class);
    }

    private String storePublicIdOtherThan(String publicId) {
        return jdbcTemplate.queryForObject(
                "SELECT public_id FROM stores WHERE public_id <> ? ORDER BY id DESC LIMIT 1", String.class, publicId);
    }

    private ResponseEntity<String> get(String path, String token) {
        return rest.exchange(url(path), HttpMethod.GET, new HttpEntity<>(headers(token)), String.class);
    }

    private ResponseEntity<String> post(String path, String body, String token) {
        return rest.exchange(url(path), HttpMethod.POST, new HttpEntity<>(body, headers(token)), String.class);
    }

    private String url(String path) {
        return "http://localhost:" + port + "/api" + path;
    }

    private static HttpHeaders headers(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HttpConstants.REQUEST_ID_HEADER, "corr-scope");
        return headers;
    }

    private static RestTemplate restTemplateThatDoesNotThrow() {
        RestTemplate template = new RestTemplate();
        template.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) {
                return false;
            }
        });
        return template;
    }
}
