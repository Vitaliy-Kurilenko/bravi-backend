package ua.com.bravi.bravi.seller.catalog;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import ua.com.bravi.bravi.AbstractPostgresIT;
import ua.com.bravi.bravi.TestDatabaseCleaner;
import ua.com.bravi.bravi.shared.common.HttpConstants;
import ua.com.bravi.bravi.shared.media.MediaStorage;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies against a real database that the gallery of a product keeps a gap-free order, that a
 * move re-sequences the whole gallery, and that the image at position 0 is reported as the primary one.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductImageOrderTest extends AbstractPostgresIT {

    private static final String SERVICE_TOKEN = "service";
    private static final String USER_TOKEN = "user";
    private static final UUID EXT_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final String STORE_BODY = "{\"name\":\"Shop\"}";
    private static final ParameterizedTypeReference<List<Map<String, Object>>> IMAGE_LIST =
            new ParameterizedTypeReference<>() {
            };

    @TestConfiguration
    static class StubJwtDecoderConfig {

        @Bean
        JwtDecoder jwtDecoder() {
            Jwt service = Jwt.withTokenValue(SERVICE_TOKEN).header("alg", "none")
                    .subject(UUID.fromString("88888888-8888-8888-8888-888888888888").toString())
                    .claim("resource_access", Map.of("backend-service", Map.of("roles", List.of("auth_service"))))
                    .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300)).build();
            Jwt user = Jwt.withTokenValue(USER_TOKEN).header("alg", "none")
                    .subject(EXT_ID.toString())
                    .claim("preferred_username", "gallery@example.com")
                    .claim("email", "gallery@example.com")
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

    /** Kept out of S3: the gallery order is decided by the application, not by the storage. */
    @MockitoBean
    private MediaStorage mediaStorage;

    private final RestTemplate rest = restTemplateThatDoesNotThrow();

    @AfterEach
    void cleanUp() {
        TestDatabaseCleaner.clean(jdbcTemplate);
    }

    @Test
    void movingAnImageResequencesTheGalleryAndDeletingItClosesTheGap() {
        when(mediaStorage.publicUrl(anyString())).thenAnswer(invocation -> "https://cdn/" + invocation.getArgument(0));

        String accountId = registerSeller();
        String storeId = createStore(accountId);
        String productId = createProduct(accountId, storeId);
        List<Long> imageIds = attachImages(productId, "a.png", "b.png", "c.png");

        assertThat(valuesOf(listImages(accountId, storeId, productId), "id"))
                .containsExactly(imageIds.get(0).intValue(), imageIds.get(1).intValue(), imageIds.get(2).intValue());
        assertThat(sortOrders(productId)).containsExactly(0, 1, 2);

        ResponseEntity<List<Map<String, Object>>> moved = rest.exchange(
                url(imagePath(productId, imageIds.get(2))), HttpMethod.PATCH,
                new HttpEntity<>("{\"sort_order\":0}", headers(USER_TOKEN, accountId, storeId)), IMAGE_LIST);
        assertThat(moved.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(valuesOf(moved.getBody(), "id"))
                .containsExactly(imageIds.get(2).intValue(), imageIds.get(0).intValue(), imageIds.get(1).intValue());
        assertThat(valuesOf(moved.getBody(), "sort_order")).containsExactly(0, 1, 2);
        assertThat(valuesOf(moved.getBody(), "is_primary")).containsExactly(true, false, false);
        assertThat(sortOrders(productId)).containsExactly(0, 1, 2);

        ResponseEntity<String> deleted = exchange(HttpMethod.DELETE,
                imagePath(productId, imageIds.get(2)), null, accountId, storeId);
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(mediaStorage).delete("product-images/x/c.png");
        assertThat(valuesOf(listImages(accountId, storeId, productId), "id"))
                .containsExactly(imageIds.get(0).intValue(), imageIds.get(1).intValue());
        assertThat(sortOrders(productId)).containsExactly(0, 1);
    }

    @Test
    void movingBeyondTheGalleryIsRejectedOnTheSortOrderField() {
        String accountId = registerSeller();
        String storeId = createStore(accountId);
        String productId = createProduct(accountId, storeId);
        List<Long> imageIds = attachImages(productId, "a.png", "b.png");

        ResponseEntity<String> response = exchange(HttpMethod.PATCH,
                imagePath(productId, imageIds.getFirst()), "{\"sort_order\":5}", accountId, storeId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("\"field\":\"sort_order\"");
        assertThat(sortOrders(productId)).containsExactly(0, 1);
    }

    /** Seeds gallery rows directly: the upload itself goes to object storage and is not under test. */
    private List<Long> attachImages(String productPublicId, String... filenames) {
        Long productId = jdbcTemplate.queryForObject(
                "SELECT id FROM store_products WHERE public_id = ?", Long.class, productPublicId);
        return IntStream.range(0, filenames.length)
                .mapToObj(position -> jdbcTemplate.queryForObject(
                        "INSERT INTO store_product_images (product_id, storage_key, content_type, sort_order, "
                                + "created_at) VALUES (?, ?, 'image/png', ?, now()) RETURNING id",
                        Long.class, productId, "product-images/x/" + filenames[position], position))
                .toList();
    }

    private List<Integer> sortOrders(String productPublicId) {
        return jdbcTemplate.queryForList(
                "SELECT i.sort_order FROM store_product_images i JOIN store_products p ON p.id = i.product_id "
                        + "WHERE p.public_id = ? ORDER BY i.sort_order", Integer.class, productPublicId);
    }

    private List<Map<String, Object>> listImages(String accountId, String storeId, String productPublicId) {
        ResponseEntity<List<Map<String, Object>>> response = rest.exchange(
                url("/sellers/products/" + productPublicId + "/images"), HttpMethod.GET,
                new HttpEntity<>(headers(USER_TOKEN, accountId, storeId)), IMAGE_LIST);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    private static List<Object> valuesOf(List<Map<String, Object>> images, String field) {
        return images.stream().map(image -> image.get(field)).toList();
    }

    private String imagePath(String productPublicId, Long imageId) {
        return "/sellers/products/" + productPublicId + "/images/" + imageId;
    }

    private String registerSeller() {
        String body = "{\"keycloakUserId\":\"" + EXT_ID + "\",\"email\":\"gallery@example.com\","
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

    private String createProduct(String accountId, String storeId) {
        Long stockStatusId = jdbcTemplate.queryForObject(
                "SELECT id FROM stock_statuses ORDER BY id LIMIT 1", Long.class);
        String body = "{\"name\":\"Widget\",\"code\":\"CODE-1\",\"sku\":\"SKU-1\","
                + "\"stock_status_id\":" + stockStatusId + ",\"price\":10.00,\"quantity\":1}";
        assertThat(exchange(HttpMethod.POST, "/sellers/products", body, accountId, storeId).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
        return jdbcTemplate.queryForObject(
                "SELECT public_id FROM store_products WHERE code = ?", String.class, "CODE-1");
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
        headers.add(HttpConstants.REQUEST_ID_HEADER, "corr-gallery");
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
