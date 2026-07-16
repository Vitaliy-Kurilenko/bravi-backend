package ua.com.bravi.bravi.dictionaries;

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
import ua.com.bravi.bravi.dictionaries.api.DictionariesApi;
import ua.com.bravi.bravi.shared.common.HttpConstants;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code GET /dictionaries[/{code}]}: authenticated-only universal reference-data read API over
 * the Flyway-seeded dictionaries; inactive items are hidden, unknown dictionary codes give 404.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DictionariesTest extends AbstractPostgresIT {

    private static final String USER_TOKEN = "user";

    @TestConfiguration
    static class StubJwtDecoderConfig {

        @Bean
        JwtDecoder jwtDecoder() {
            Jwt user = Jwt.withTokenValue(USER_TOKEN).header("alg", "none")
                    .subject(UUID.fromString("66666666-6666-6666-6666-666666666666").toString())
                    .claim("resource_access", Map.of("backend-service", Map.of("roles", List.of("bravi_user"))))
                    .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300)).build();
            return token -> user;
        }
    }

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DictionariesApi dictionariesApi;

    private final RestTemplate rest = restTemplateThatDoesNotThrow();

    @AfterEach
    void restoreSeedData() {
        jdbcTemplate.update("UPDATE dictionary_items SET active = TRUE");
    }

    @Test
    void anonymousRequestGets401() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpConstants.REQUEST_ID_HEADER, "corr-dict");

        ResponseEntity<String> response = rest.exchange(
                url("/dictionaries"), HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void listsAllDictionaries() {
        ResponseEntity<String> response = get("/dictionaries");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .contains("\"code\":\"CURRENCY\"")
                .contains("\"code\":\"LANGUAGE\"")
                .contains("\"code\":\"WEIGHT_UNIT\"")
                .contains("\"code\":\"DIMENSION_UNIT\"")
                .contains("\"code\":\"TIMEZONE\"")
                .contains("\"code\":\"COUNTRY\"");
    }

    @Test
    void returnsActiveItemsOrderedWithMeta() {
        ResponseEntity<String> response = get("/dictionaries/CURRENCY");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        // UAH has the lowest sort_order → first element.
        assertThat(response.getBody())
                .startsWith("[{\"code\":\"UAH\"")
                .contains("\"symbol\":\"₴\"");
    }

    @Test
    void unknownDictionaryGets404() {
        ResponseEntity<String> response = get("/dictionaries/UNKNOWN");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("Dictionary not found");
    }

    @Test
    void inactiveItemsAreExcluded() {
        jdbcTemplate.update("UPDATE dictionary_items SET active = FALSE WHERE code = 'CAD'");

        ResponseEntity<String> response = get("/dictionaries/CURRENCY");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).doesNotContain("\"CAD\"");
    }

    @Test
    void isActiveItemValidatesCodes() {
        assertThat(dictionariesApi.isActiveItem("CURRENCY", "UAH")).isTrue();
        assertThat(dictionariesApi.isActiveItem("CURRENCY", "XXX")).isFalse();
        assertThat(dictionariesApi.isActiveItem("NOPE", "UAH")).isFalse();
    }

    private ResponseEntity<String> get(String path) {
        return rest.exchange(url(path), HttpMethod.GET, new HttpEntity<>(headers()), String.class);
    }

    private String url(String path) {
        return "http://localhost:" + port + "/api" + path;
    }

    private static HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(USER_TOKEN);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HttpConstants.REQUEST_ID_HEADER, "corr-dict");
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
