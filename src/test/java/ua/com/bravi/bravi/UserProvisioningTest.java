package ua.com.bravi.bravi;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.client.RestTemplate;
import ua.com.bravi.bravi.identity.domain.UserStatus;
import ua.com.bravi.bravi.identity.persistence.IUserEntityRepository;
import ua.com.bravi.bravi.identity.persistence.entity.UserEntity;
import ua.com.bravi.bravi.shared.common.HttpConstants;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * With explicit registration, {@code /users/context} resolves the user *lookup-only*:
 * an unknown identity is never auto-provisioned; a registered one is returned.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserProvisioningTest extends AbstractPostgresIT {

    private static final UUID EXT_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @TestConfiguration
    static class StubJwtDecoderConfig {

        @Bean
        JwtDecoder jwtDecoder() {
            Jwt jwt = Jwt.withTokenValue("stub")
                    .header("alg", "none")
                    .subject(EXT_ID.toString())
                    .claim("preferred_username", "jit.user")
                    .claim("email", "jit@example.com")
                    .claim("given_name", "Jit")
                    .claim("family_name", "User")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(300))
                    .build();
            return token -> jwt;
        }
    }

    @LocalServerPort
    private int port;

    @Autowired
    private IUserEntityRepository repository;

    private final RestTemplate rest = new RestTemplate();

    @BeforeEach
    void cleanTable() {
        repository.deleteAll();
    }

    private ResponseEntity<String> callUserContext() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("any-token");
        headers.add(HttpConstants.REQUEST_ID_HEADER, "corr-it");
        return rest.exchange(
                "http://localhost:" + port + "/api/users/context",
                org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);
    }

    @Test
    void unknownUserIsNotProvisioned() {
        ResponseEntity<String> response = callUserContext();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(repository.count()).isZero();
    }

    @Test
    void registeredUserIsResolved() {
        UserEntity seeded = new UserEntity();
        seeded.setPublicId("usr_seed");
        seeded.setExtId(EXT_ID);
        seeded.setFirstName("Jit");
        seeded.setEmail("jit@example.com");
        seeded.setStatus(UserStatus.ACTIVE);
        repository.save(seeded);

        ResponseEntity<String> response = callUserContext();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .contains("\"first_name\":\"Jit\"")
                .contains("\"status\":\"ACTIVE\"");
        assertThat(repository.count()).isEqualTo(1);
    }
}
