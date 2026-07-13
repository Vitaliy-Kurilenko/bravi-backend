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
 * {@code GET /accounts}: unknown identity → 404 (no JIT); a registered seller gets their accounts
 * with role + onboarding status, and {@code email_verified} is synced false→true from the JWT.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AccountsTest extends AbstractPostgresIT {

    private static final UUID SELLER_EXT_ID = UUID.fromString("77777777-7777-7777-7777-777777777777");
    private static final String SERVICE_TOKEN = "service";
    private static final String USER_TOKEN = "user";
    private static final String UNKNOWN_TOKEN = "unknown";

    private static final String REGISTRATION_BODY =
            "{\"keycloakUserId\":\"" + SELLER_EXT_ID + "\",\"email\":\"me@example.com\","
                    + "\"firstName\":\"Mia\",\"lastName\":\"Owner\"}";

    @TestConfiguration
    static class StubJwtDecoderConfig {

        @Bean
        JwtDecoder jwtDecoder() {
            Jwt service = Jwt.withTokenValue(SERVICE_TOKEN).header("alg", "none")
                    .subject(UUID.fromString("99999999-9999-9999-9999-999999999999").toString())
                    .claim("resource_access", Map.of("backend-service", Map.of("roles", List.of("auth_service"))))
                    .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300)).build();
            Jwt user = Jwt.withTokenValue(USER_TOKEN).header("alg", "none")
                    .subject(SELLER_EXT_ID.toString())
                    .claim("email", "me@example.com").claim("email_verified", true)
                    .claim("given_name", "Mia").claim("family_name", "Owner")
                    .claim("resource_access", Map.of("backend-service", Map.of("roles", List.of("bravi_user"))))
                    .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300)).build();
            Jwt unknown = Jwt.withTokenValue(UNKNOWN_TOKEN).header("alg", "none")
                    .subject(UUID.fromString("88888888-8888-8888-8888-888888888888").toString())
                    .claim("resource_access", Map.of("backend-service", Map.of("roles", List.of("bravi_user"))))
                    .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300)).build();
            return token -> switch (token) {
                case SERVICE_TOKEN -> service;
                case UNKNOWN_TOKEN -> unknown;
                default -> user;
            };
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
    void unknownUserGets404() {
        ResponseEntity<String> response = get("/accounts", UNKNOWN_TOKEN);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void registeredUserGetsAccountsAndEmailVerifiedIsSynced() {
        assertThat(post("/internal/registrations/seller", REGISTRATION_BODY, SERVICE_TOKEN).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT email_verified FROM users", Boolean.class)).isFalse();

        ResponseEntity<String> response = get("/accounts", USER_TOKEN);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .contains("\"type\":\"SELLER\"")
                .contains("\"role\":\"SELLER_OWNER\"")
                .contains("\"onboarding_status\":\"NOT_STARTED\"")
                .contains("\"email_verified\":true");

        // JWT reported email_verified=true → the stored flag is upgraded.
        assertThat(jdbcTemplate.queryForObject(
                "SELECT email_verified FROM users", Boolean.class)).isTrue();
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
        headers.add(HttpConstants.REQUEST_ID_HEADER, "corr-me");
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
