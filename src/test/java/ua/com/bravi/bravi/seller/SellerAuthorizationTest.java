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
 * End-to-end DB-backed RBAC on the seller vertical after the explicit-registration switch:
 * a bravi_user can only write once the Auth Service has registered them (service token
 * → {@code /internal/registrations/seller}), which creates the account + SELLER_OWNER membership.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SellerAuthorizationTest extends AbstractPostgresIT {

    private static final UUID SELLER_EXT_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final String SERVICE_TOKEN = "service";
    private static final String USER_TOKEN = "user";

    private static final String STORE_BODY = "{\"name\":\"Shop\"}";
    private static final String REGISTRATION_BODY =
            "{\"keycloakUserId\":\"" + SELLER_EXT_ID + "\",\"email\":\"owner@example.com\","
                    + "\"firstName\":\"Olga\",\"lastName\":\"Owner\"}";

    @TestConfiguration
    static class StubJwtDecoderConfig {

        @Bean
        JwtDecoder jwtDecoder() {
            Jwt serviceToken = Jwt.withTokenValue(SERVICE_TOKEN)
                    .header("alg", "none")
                    .subject(UUID.fromString("99999999-9999-9999-9999-999999999999").toString())
                    .claim("resource_access", Map.of("backend-service", Map.of("roles", List.of("auth_service"))))
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(300))
                    .build();
            Jwt userToken = Jwt.withTokenValue(USER_TOKEN)
                    .header("alg", "none")
                    .subject(SELLER_EXT_ID.toString())
                    .claim("preferred_username", "seller.owner")
                    .claim("email", "owner@example.com")
                    .claim("given_name", "Olga")
                    .claim("family_name", "Owner")
                    .claim("resource_access", Map.of("backend-service", Map.of("roles", List.of("bravi_user"))))
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(300))
                    .build();
            return token -> SERVICE_TOKEN.equals(token) ? serviceToken : userToken;
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
    void writesAreDeniedUntilTheSellerIsRegistered() {
        // 1. A bravi_user token with no registered business context has no STORE permission → 403.
        assertThat(post("/sellers/onboarding/store", STORE_BODY, USER_TOKEN, "acc_missing").getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);

        // 2. Auth Service registers the seller (service-account token) → account + SELLER_OWNER membership.
        assertThat(post("/internal/registrations/seller", REGISTRATION_BODY, SERVICE_TOKEN).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
        String accountId = jdbcTemplate.queryForObject("SELECT public_id FROM accounts", String.class);

        // 3. The same user token now carries STORE_READ/STORE_WRITE via the SELLER_OWNER role.
        assertThat(get("/sellers/onboarding", USER_TOKEN, accountId).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(post("/sellers/onboarding/store", STORE_BODY, USER_TOKEN, accountId).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void internalRegistrationRejectsUserToken() {
        assertThat(post("/internal/registrations/seller", REGISTRATION_BODY, USER_TOKEN).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    private ResponseEntity<String> get(String path, String token, String accountId) {
        return rest.exchange(url(path), HttpMethod.GET, new HttpEntity<>(headers(token, accountId)), String.class);
    }

    private ResponseEntity<String> post(String path, String body, String token) {
        return post(path, body, token, null);
    }

    private ResponseEntity<String> post(String path, String body, String token, String accountId) {
        return rest.exchange(url(path), HttpMethod.POST, new HttpEntity<>(body, headers(token, accountId)), String.class);
    }

    private String url(String path) {
        return "http://localhost:" + port + "/api" + path;
    }

    private static HttpHeaders headers(String token, String accountId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HttpConstants.REQUEST_ID_HEADER, "corr-authz");
        if (accountId != null) {
            headers.add(HttpConstants.ACCOUNT_ID_HEADER, accountId);
        }
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
