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
import ua.com.bravi.bravi.shared.common.HttpConstants;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end enforcement of DB-backed RBAC on the seller vertical: a {@code role_seller}
 * user passes the coarse HTTP gate but every write is additionally gated by
 * {@code hasPermission(resource, action)} against the current account's permission set.
 * Also exercises the onboarding path (account + owner membership + SELLER_OWNER role assignment).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SellerAuthorizationTest extends AbstractPostgresIT {

    private static final UUID EXT_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

    @TestConfiguration
    static class StubJwtDecoderConfig {

        @Bean
        JwtDecoder jwtDecoder() {
            Jwt jwt = Jwt.withTokenValue("stub")
                    .header("alg", "none")
                    .subject(EXT_ID.toString())
                    .claim("preferred_username", "seller.owner")
                    .claim("email", "owner@example.com")
                    .claim("given_name", "Olga")
                    .claim("family_name", "Owner")
                    .claim("realm_access", Map.of("roles", List.of("role_seller")))
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(300))
                    .build();
            return token -> jwt;
        }
    }

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RestTemplate rest = restTemplateThatDoesNotThrow();

    @AfterEach
    void cleanUp() {
        // Real-HTTP flow commits rows to the shared container; drop them (FK-safe) so
        // sibling ITs (e.g. UserProvisioningTest wiping users) are not affected.
        jdbcTemplate.execute("TRUNCATE TABLE users, accounts RESTART IDENTITY CASCADE");
    }

    @Test
    void writesAreDeniedUntilOwnerAccountIsProvisioned() {
        // 1. role_seller alone is not enough: no seller account → no STORE_WRITE → 403.
        assertThat(post("/seller/stores", "{\"name\":\"Shop\",\"timezone\":\"UTC\",\"currency\":\"UAH\",\"allow_return\":true}")
                .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // 2. Onboard: creates account + owner membership + SELLER_OWNER role (all seller permissions).
        assertThat(post("/seller/accounts", "{\"legalName\":\"Acme LLC\"}")
                .getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // 3. Now the same write is permitted.
        assertThat(post("/seller/stores", "{\"name\":\"Shop\",\"timezone\":\"UTC\",\"currency\":\"UAH\",\"allow_return\":true}")
                .getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // 4. Reads on the owned store/catalog are permitted too.
        assertThat(get("/seller/stores").getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(get("/seller/products").getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private ResponseEntity<String> get(String path) {
        return rest.exchange(url(path), HttpMethod.GET, new HttpEntity<>(headers()), String.class);
    }

    private ResponseEntity<String> post(String path, String body) {
        return rest.exchange(url(path), HttpMethod.POST, new HttpEntity<>(body, headers()), String.class);
    }

    private String url(String path) {
        return "http://localhost:" + port + "/api" + path;
    }

    private static HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("any-token");
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HttpConstants.REQUEST_ID_HEADER, "corr-authz");
        return headers;
    }

    private static RestTemplate restTemplateThatDoesNotThrow() {
        RestTemplate template = new RestTemplate();
        // status codes (incl. 403) are asserted by the test, so never treat them as errors
        template.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) {
                return false;
            }
        });
        return template;
    }
}
