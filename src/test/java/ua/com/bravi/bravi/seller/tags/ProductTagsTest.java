package ua.com.bravi.bravi.seller.tags;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ParameterizedTypeReference;
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
import ua.com.bravi.bravi.shared.util.ValidationPatterns;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Walks tagging from the product's side against a real database: what a submitted tag means, when a
 * name mints a tag, and how the list filter narrows by them.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductTagsTest extends AbstractPostgresIT {

    private static final String SERVICE_TOKEN = "service";
    private static final String USER_TOKEN = "user";
    private static final UUID EXT_ID = UUID.fromString("77777777-7777-7777-7777-777777777777");
    private static final ParameterizedTypeReference<List<Map<String, Object>>> LIST_OF_MAPS =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<Map<String, Object>> MAP =
            new ParameterizedTypeReference<>() {
            };

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
                    .claim("preferred_username", "tags@example.com")
                    .claim("email", "tags@example.com")
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

    @AfterEach
    void cleanUp() {
        TestDatabaseCleaner.clean(jdbcTemplate);
    }

    @Test
    void namingATagOnAProductCreatesItAndPinsItInOneCall() {
        setUpStore();
        String product = createProduct("T-1", """
                ,"tags":[{"name":"Хіт"},{"name":"Розпродаж"}]""");

        assertThat(countTags()).isEqualTo(2);
        assertThat(productTags(product)).extracting(tag -> tag.get("name"))
                .containsExactlyInAnyOrder("Хіт", "Розпродаж");
        assertThat(productTags(product)).allSatisfy(tag ->
                assertThat((String) tag.get("public_id")).startsWith("tag_"));
    }

    /** The normalized name is the real key, so spellings of one word must not become two tags. */
    @Test
    void spellingsOfOneNameCollapseIntoASingleTagAndASingleLink() {
        setUpStore();
        String product = createProduct("T-2", """
                ,"tags":[{"name":"  Хіт  "},{"name":"хіт"},{"name":"ХІТ"}]""");

        assertThat(countTags()).isEqualTo(1);
        assertThat(countLinks()).isEqualTo(1);
        assertThat(productTags(product)).extracting(tag -> tag.get("name")).containsExactly("Хіт");
    }

    /** A tag minted from a product card is drawable straight away, without a trip to the dictionary. */
    @Test
    void aTagMintedOnAProductCardArrivesWithAColour() {
        setUpStore();
        String product = createProduct("T-21", """
                ,"tags":[{"name":"Хіт"}]""");

        for (Map<String, Object> tag : List.of(tagsOf(getProduct(product)).getFirst(),
                tagsOf(dataOf(searchProducts("")).getFirst()).getFirst())) {
            assertThat((String) tag.get("color")).isNotNull()
                    .matches(ValidationPatterns.HEX_COLOR.pattern());
        }
    }

    @Test
    void anExistingTagIsPinnedByItsIdAndNeverRenamed() {
        setUpStore();
        String tagId = createTag("Хіт");
        String product = createProduct("T-3", ",\"tags\":[{\"id\":\"%s\",\"name\":\"Ігнорується\"}]"
                .formatted(tagId));

        assertThat(countTags()).isEqualTo(1);
        assertThat(productTags(product)).extracting(tag -> tag.get("name")).containsExactly("Хіт");
    }

    @Test
    void anUnknownTagIdIsNotFound() {
        setUpStore();
        String product = createProduct("T-4", "");

        assertThat(replace(product, """
                {"tags":[{"id":"tag_nope"}]}""").getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void anEntryWithoutAnIdOrANameIsRejectedAgainstItsOwnPosition() {
        setUpStore();
        String product = createProduct("T-5", "");

        ResponseEntity<Map<String, Object>> response = rest.exchange(
                url("/sellers/products/" + product + "/tags"), HttpMethod.PUT,
                new HttpEntity<>("""
                        {"tags":[{"name":"Хіт"},{}]}""", headers()), MAP);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(firstErrorField(response.getBody())).isEqualTo("tags[1]");
    }

    @Test
    void anOverlongNameIsRejected() {
        setUpStore();
        String product = createProduct("T-6", "");

        assertThat(replace(product, """
                {"tags":[{"name":"%s"}]}""".formatted("a".repeat(65))).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void replacingWritesTheDifferenceAndAnEmptyListClearsTheTags() {
        setUpStore();
        String product = createProduct("T-7", """
                ,"tags":[{"name":"Хіт"},{"name":"Розпродаж"}]""");

        replaceOk(product, """
                {"tags":[{"name":"Розпродаж"},{"name":"Новинка"}]}""");
        assertThat(productTags(product)).extracting(tag -> tag.get("name"))
                .containsExactlyInAnyOrder("Розпродаж", "Новинка");
        // The tag that was unpinned stays in the dictionary; only the link went away.
        assertThat(countTags()).isEqualTo(3);

        replaceOk(product, """
                {"tags":[]}""");
        assertThat(productTags(product)).isEmpty();
        assertThat(countTags()).isEqualTo(3);
    }

    @Test
    void patchingAProductWithoutTagsLeavesThemAlone() {
        setUpStore();
        String product = createProduct("T-8", """
                ,"tags":[{"name":"Хіт"}]""");

        assertThat(exchange(HttpMethod.PATCH, "/sellers/products/" + product, """
                {"name":"Renamed"}""").getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        assertThat(productTags(product)).extracting(tag -> tag.get("name")).containsExactly("Хіт");
    }

    @Test
    void patchingWithAnEmptyListClearsThem() {
        setUpStore();
        String product = createProduct("T-9", """
                ,"tags":[{"name":"Хіт"}]""");

        assertThat(exchange(HttpMethod.PATCH, "/sellers/products/" + product, """
                {"tags":[]}""").getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        assertThat(productTags(product)).isEmpty();
    }

    /** Badges are read in the listing, so the page carries them just like the single read does. */
    @Test
    void bothThePageAndTheSingleReadCarryTheTags() {
        setUpStore();
        String product = createProduct("T-10", """
                ,"tags":[{"name":"Хіт"}]""");

        assertThat(tagsOf(getProduct(product))).extracting(tag -> tag.get("name")).containsExactly("Хіт");

        Map<String, Object> listed = dataOf(searchProducts("")).getFirst();
        assertThat(tagsOf(listed)).extracting(tag -> tag.get("name")).containsExactly("Хіт");
        // On a product a tag is a bare reference: what the badge is drawn from and nothing else.
        assertThat(tagsOf(listed).getFirst()).containsOnlyKeys("id", "name", "color");
    }

    @Test
    void theListFilterAcceptsAnyOfTheTagsByDefault() {
        setUpStore();
        String hit = createTag("Хіт");
        String sale = createTag("Розпродаж");
        createProduct("T-11", ",\"tags\":[{\"id\":\"%s\"}]".formatted(hit));
        createProduct("T-12", ",\"tags\":[{\"id\":\"%s\"}]".formatted(sale));
        createProduct("T-13", "");

        assertThat(dataOf(searchProducts("?tag_ids=" + hit + "," + sale)))
                .extracting(product -> product.get("code")).containsExactlyInAnyOrder("T-11", "T-12");
        assertThat(dataOf(searchProducts("?tag_ids=" + hit + "," + sale + "&tags_match=any")))
                .hasSize(2);
    }

    @Test
    void matchingAllDemandsEveryTag() {
        setUpStore();
        String hit = createTag("Хіт");
        String sale = createTag("Розпродаж");
        createProduct("T-14", ",\"tags\":[{\"id\":\"%s\"},{\"id\":\"%s\"}]".formatted(hit, sale));
        createProduct("T-15", ",\"tags\":[{\"id\":\"%s\"}]".formatted(hit));

        Map<String, Object> page = searchProducts("?tag_ids=" + hit + "," + sale + "&tags_match=all");

        assertThat(dataOf(page)).extracting(product -> product.get("code")).containsExactly("T-14");
        assertThat(page.get("count")).isEqualTo(1);
    }

    @Test
    void anUnknownTagInTheFilterIsNotFoundRatherThanIgnored() {
        setUpStore();
        createProduct("T-16", "");

        assertThat(rest.exchange(url("/sellers/products?tag_ids=tag_nope"), HttpMethod.GET,
                new HttpEntity<>(headers()), String.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void bulkAddsRemovesAndReplacesReportingHowManyProductsChanged() {
        setUpStore();
        String first = createProduct("T-17", "");
        String second = createProduct("T-18", "");

        assertThat(bulk("""
                {"product_ids":["%s","%s"],"tags":[{"name":"Хіт"}]}""".formatted(first, second)))
                .containsEntry("updated", 2);
        // Already pinned, so the second run changes nothing.
        assertThat(bulk("""
                {"product_ids":["%s","%s"],"tags":[{"name":"Хіт"}],"mode":"ADD"}""".formatted(first, second)))
                .containsEntry("updated", 0);

        assertThat(bulk("""
                {"product_ids":["%s"],"tags":[{"name":"Хіт"}],"mode":"REMOVE"}""".formatted(first)))
                .containsEntry("updated", 1);
        assertThat(productTags(first)).isEmpty();
        assertThat(productTags(second)).hasSize(1);

        assertThat(bulk("""
                {"product_ids":["%s"],"tags":[{"name":"Новинка"}],"mode":"REPLACE"}""".formatted(second)))
                .containsEntry("updated", 1);
        assertThat(productTags(second)).extracting(tag -> tag.get("name")).containsExactly("Новинка");
    }

    /** Removing by a name nothing matches has nothing to unpin, so it must not fill the dictionary. */
    @Test
    void bulkRemoveNeverCreatesATag() {
        setUpStore();
        String product = createProduct("T-19", "");

        assertThat(bulk("""
                {"product_ids":["%s"],"tags":[{"name":"Невідомий"}],"mode":"REMOVE"}""".formatted(product)))
                .containsEntry("updated", 0);
        assertThat(countTags()).isZero();
    }

    @Test
    void deletingAProductTakesItsLinksWithIt() {
        setUpStore();
        String product = createProduct("T-20", """
                ,"tags":[{"name":"Хіт"}]""");

        assertThat(exchange(HttpMethod.DELETE, "/sellers/products/" + product, null).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);

        assertThat(countLinks()).isZero();
        assertThat(countTags()).isEqualTo(1);
    }

    // --- helpers -------------------------------------------------------------------------------

    private void setUpStore() {
        accountId = registerSeller();
        storeId = createStore();
    }

    private String registerSeller() {
        String body = "{\"keycloakUserId\":\"" + EXT_ID + "\",\"email\":\"tags@example.com\","
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

    private String createProduct(String code, String tagsFragment) {
        Long stockStatusId = jdbcTemplate.queryForObject(
                "SELECT id FROM stock_statuses ORDER BY id LIMIT 1", Long.class);
        String body = "{\"name\":\"Widget\",\"code\":\"" + code + "\",\"stock_status_id\":" + stockStatusId
                + ",\"price\":100,\"quantity\":1" + tagsFragment + "}";
        assertThat(exchange(HttpMethod.POST, "/sellers/products", body).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
        return jdbcTemplate.queryForObject(
                "SELECT public_id FROM store_products WHERE code = ?", String.class, code);
    }

    private String createTag(String name) {
        ResponseEntity<Map<String, Object>> response = rest.exchange(
                url("/sellers/tags/products"), HttpMethod.POST,
                new HttpEntity<>("{\"name\":\"" + name + "\"}", headers()), MAP);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return (String) response.getBody().get("public_id");
    }

    private List<Map<String, Object>> productTags(String productPublicId) {
        return rest.exchange(url("/sellers/products/" + productPublicId + "/tags"), HttpMethod.GET,
                new HttpEntity<>(headers()), LIST_OF_MAPS).getBody();
    }

    private ResponseEntity<String> replace(String productPublicId, String body) {
        return exchange(HttpMethod.PUT, "/sellers/products/" + productPublicId + "/tags", body);
    }

    private void replaceOk(String productPublicId, String body) {
        assertThat(replace(productPublicId, body).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private Map<String, Object> bulk(String body) {
        ResponseEntity<Map<String, Object>> response = rest.exchange(url("/sellers/product-tags/bulk"),
                HttpMethod.POST, new HttpEntity<>(body, headers()), MAP);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    private Map<String, Object> getProduct(String productPublicId) {
        return rest.exchange(url("/sellers/products/" + productPublicId), HttpMethod.GET,
                new HttpEntity<>(headers()), MAP).getBody();
    }

    private Map<String, Object> searchProducts(String queryString) {
        return rest.exchange(url("/sellers/products" + queryString), HttpMethod.GET,
                new HttpEntity<>(headers()), MAP).getBody();
    }

    private int countTags() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM store_tags", Integer.class);
    }

    private int countLinks() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM store_product_tags", Integer.class);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> dataOf(Map<String, Object> page) {
        return (List<Map<String, Object>>) page.get("data");
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> tagsOf(Map<String, Object> product) {
        return (List<Map<String, Object>>) product.get("tags");
    }

    @SuppressWarnings("unchecked")
    private static String firstErrorField(Map<String, Object> problem) {
        List<Map<String, Object>> errors = (List<Map<String, Object>>) problem.get("errors");
        return (String) errors.getFirst().get("field");
    }

    private ResponseEntity<String> exchange(HttpMethod method, String path, String body) {
        return rest.exchange(url(path), method, new HttpEntity<>(body, headers()), String.class);
    }

    private HttpHeaders headers() {
        return headers(USER_TOKEN, accountId, storeId);
    }

    private static HttpHeaders headers(String token, String accountId, String storeId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HttpConstants.REQUEST_ID_HEADER, "corr-tags");
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
