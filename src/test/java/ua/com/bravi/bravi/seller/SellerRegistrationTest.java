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
 * Verifies the internal seller-registration endpoint: a Keycloak service-account token
 * ({@code auth_service}) creates User + SELLER Account (PENDING_ONBOARDING) +
 * SellerAccount (NOT_STARTED) + owner Membership, and repeat calls are idempotent.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SellerRegistrationTest extends AbstractPostgresIT {

    private static final UUID KEYCLOAK_USER_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final String BODY =
            "{\"keycloakUserId\":\"" + KEYCLOAK_USER_ID + "\",\"email\":\"newseller@example.com\","
                    + "\"firstName\":\"Nadia\",\"lastName\":\"Seller\"}";

    @TestConfiguration
    static class StubJwtDecoderConfig {

        @Bean
        JwtDecoder jwtDecoder() {
            Jwt serviceToken = Jwt.withTokenValue("service")
                    .header("alg", "none")
                    .subject(UUID.fromString("99999999-9999-9999-9999-999999999999").toString())
                    .claim("resource_access", Map.of("backend-service", Map.of("roles", List.of("auth_service"))))
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(300))
                    .build();
            return token -> serviceToken;
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
    void registrationCreatesSellerBusinessContext() {
        ResponseEntity<String> response = register();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody())
                .contains("\"accountType\":\"SELLER\"")
                .contains("\"accountStatus\":\"PENDING_ONBOARDING\"")
                .contains("\"onboardingStatus\":\"NOT_STARTED\"")
                .contains("\"userId\":\"usr_")
                .contains("\"accountId\":\"acc_")
                .contains("\"membershipId\":\"mem_");

        assertThat(count("users")).isEqualTo(1);
        assertThat(count("accounts")).isEqualTo(1);
        assertThat(count("seller_accounts")).isEqualTo(1);
        assertThat(count("memberships")).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM accounts", String.class)).isEqualTo("PENDING_ONBOARDING");
    }

    @Test
    void registrationIsIdempotent() {
        assertThat(register().getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(register().getStatusCode()).isEqualTo(HttpStatus.CREATED);

        assertThat(count("users")).isEqualTo(1);
        assertThat(count("accounts")).isEqualTo(1);
        assertThat(count("memberships")).isEqualTo(1);
    }

    private ResponseEntity<String> register() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("service");
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HttpConstants.REQUEST_ID_HEADER, "corr-reg");
        return rest.exchange(
                "http://localhost:" + port + "/api/internal/registrations/seller",
                HttpMethod.POST, new HttpEntity<>(BODY, headers), String.class);
    }

    private int count(String table) {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM " + table, Integer.class);
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
