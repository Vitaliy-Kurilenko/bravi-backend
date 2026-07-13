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
 * End-to-end seller onboarding (spec §5): register → create DRAFT store (+ default settings + manual
 * channel) → replace contacts → complete (account ACTIVE, onboarding COMPLETED, store ACTIVE);
 * plus the guard rails EMAIL_NOT_VERIFIED and ONBOARDING_INCOMPLETE.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SellerOnboardingTest extends AbstractPostgresIT {

    private static final UUID SELLER_EXT_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final String SERVICE_TOKEN = "service";
    private static final String VERIFIED_TOKEN = "verified";
    private static final String UNVERIFIED_TOKEN = "unverified";

    private static final String REGISTRATION_BODY =
            "{\"keycloakUserId\":\"" + SELLER_EXT_ID + "\",\"email\":\"onb@example.com\","
                    + "\"firstName\":\"Olga\",\"lastName\":\"Owner\"}";

    @TestConfiguration
    static class StubJwtDecoderConfig {

        @Bean
        JwtDecoder jwtDecoder() {
            Jwt service = Jwt.withTokenValue(SERVICE_TOKEN).header("alg", "none")
                    .subject(UUID.fromString("99999999-9999-9999-9999-999999999999").toString())
                    .claim("resource_access", Map.of("backend-service", Map.of("roles", List.of("auth_service"))))
                    .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300)).build();
            Jwt verified = Jwt.withTokenValue(VERIFIED_TOKEN).header("alg", "none")
                    .subject(SELLER_EXT_ID.toString())
                    .claim("email", "onb@example.com").claim("email_verified", true)
                    .claim("resource_access", Map.of("backend-service", Map.of("roles", List.of("bravi_user"))))
                    .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300)).build();
            Jwt unverified = Jwt.withTokenValue(UNVERIFIED_TOKEN).header("alg", "none")
                    .subject(SELLER_EXT_ID.toString())
                    .claim("email", "onb@example.com").claim("email_verified", false)
                    .claim("resource_access", Map.of("backend-service", Map.of("roles", List.of("bravi_user"))))
                    .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300)).build();
            return token -> switch (token) {
                case SERVICE_TOKEN -> service;
                case UNVERIFIED_TOKEN -> unverified;
                default -> verified;
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
    void fullOnboardingActivatesAccountAndStore() {
        String accountId = registerAndGetAccountPublicId();

        // Create DRAFT store — default settings + manual channel are provisioned alongside.
        ResponseEntity<String> created = post(
                onboarding(accountId) + "/store", "{\"name\":\"Olga's Shop\"}", VERIFIED_TOKEN);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody()).contains("\"status\":\"DRAFT\"");

        assertThat(jdbcTemplate.queryForObject("SELECT status FROM stores", String.class)).isEqualTo("DRAFT");
        assertThat(jdbcTemplate.queryForObject("SELECT type FROM sales_channels", String.class)).isEqualTo("MANUAL");
        assertThat(jdbcTemplate.queryForObject("SELECT onboarding_status FROM seller_accounts", String.class))
                .isEqualTo("IN_PROGRESS");

        // Replace contacts.
        ResponseEntity<String> contacts = exchange(HttpMethod.PUT, onboarding(accountId) + "/store/contacts",
                "{\"contacts\":[{\"type\":\"EMAIL\",\"value\":\"shop@example.com\"}]}", VERIFIED_TOKEN);
        assertThat(contacts.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Complete.
        ResponseEntity<String> completed = post(onboarding(accountId) + "/complete", "", VERIFIED_TOKEN);
        assertThat(completed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(completed.getBody())
                .contains("\"account_status\":\"ACTIVE\"")
                .contains("\"onboarding_status\":\"COMPLETED\"");

        assertThat(jdbcTemplate.queryForObject("SELECT status FROM accounts", String.class)).isEqualTo("ACTIVE");
        assertThat(jdbcTemplate.queryForObject("SELECT onboarding_status FROM seller_accounts", String.class))
                .isEqualTo("COMPLETED");
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM stores", String.class)).isEqualTo("ACTIVE");
    }

    @Test
    void completeBeforeEmailVerifiedIsForbidden() {
        String accountId = registerAndGetAccountPublicId();

        ResponseEntity<String> response = post(onboarding(accountId) + "/complete", "", UNVERIFIED_TOKEN);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).contains("Email not verified");
    }

    @Test
    void completeWithoutStoreIsUnprocessable() {
        String accountId = registerAndGetAccountPublicId();

        ResponseEntity<String> response = post(onboarding(accountId) + "/complete", "", VERIFIED_TOKEN);

        assertThat(response.getStatusCode().value()).isEqualTo(422);
        assertThat(response.getBody()).contains("store");
    }

    private String registerAndGetAccountPublicId() {
        assertThat(post("/internal/registrations/seller", REGISTRATION_BODY, SERVICE_TOKEN).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
        return jdbcTemplate.queryForObject("SELECT public_id FROM accounts", String.class);
    }

    private static String onboarding(String accountPublicId) {
        return "/accounts/" + accountPublicId + "/seller/onboarding";
    }

    private ResponseEntity<String> post(String path, String body, String token) {
        return exchange(HttpMethod.POST, path, body, token);
    }

    private ResponseEntity<String> exchange(HttpMethod method, String path, String body, String token) {
        return rest.exchange(url(path), method, new HttpEntity<>(body, headers(token)), String.class);
    }

    private String url(String path) {
        return "http://localhost:" + port + "/api" + path;
    }

    private static HttpHeaders headers(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HttpConstants.REQUEST_ID_HEADER, "corr-onb");
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
