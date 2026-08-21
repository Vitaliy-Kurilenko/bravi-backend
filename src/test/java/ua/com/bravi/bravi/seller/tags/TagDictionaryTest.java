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
 * Walks the tag dictionary against a real database: how names deduplicate, what a rename may and
 * may not do, and what happens to the things a deleted or merged tag was labelling.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TagDictionaryTest extends AbstractPostgresIT {

    private static final String SERVICE_TOKEN = "service";
    private static final String USER_TOKEN = "user";
    private static final UUID EXT_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
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
                    .claim("preferred_username", "dictionary@example.com")
                    .claim("email", "dictionary@example.com")
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
    void aTagIsCreatedActiveWithNothingLabelledYet() {
        setUpStore();

        ResponseEntity<Map<String, Object>> response = createTagResponse("products", "Хіт");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody())
                .containsEntry("name", "Хіт")
                .containsEntry("status", "ACTIVE")
                .containsEntry("usage_count", 0);
        assertThat((String) response.getBody().get("public_id")).startsWith("tag_");
    }

    /** Nobody has to pick a colour, but every tag comes out with one. */
    @Test
    void aTagCreatedWithoutAColourIsGivenOne() {
        setUpStore();

        String color = (String) createTagResponse("products", "Хіт").getBody().get("color");

        assertThat(color).isNotNull();
        assertThat(ValidationPatterns.HEX_COLOR.matcher(color).matches())
                .as("colour %s is in the stored form", color)
                .isTrue();
    }

    @Test
    void aSubmittedColourIsStoredInTheCanonicalForm() {
        setUpStore();

        ResponseEntity<Map<String, Object>> response = rest.exchange(
                url("/sellers/tags/products"), HttpMethod.POST,
                new HttpEntity<>("""
                        {"name":"Хіт","color":"#e5484d"}""", headers()), MAP);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).containsEntry("color", "#E5484D");
    }

    @Test
    void theShorthandColourIsAccepted() {
        setUpStore();

        ResponseEntity<Map<String, Object>> response = rest.exchange(
                url("/sellers/tags/products"), HttpMethod.POST,
                new HttpEntity<>("""
                        {"name":"Хіт","color":"#f80"}""", headers()), MAP);

        assertThat(response.getBody()).containsEntry("color", "#FF8800");
    }

    @Test
    void aValueThatIsNotAColourIsRejectedAgainstItsField() {
        setUpStore();

        ResponseEntity<Map<String, Object>> response = rest.exchange(
                url("/sellers/tags/products"), HttpMethod.POST,
                new HttpEntity<>("""
                        {"name":"Хіт","color":"red"}""", headers()), MAP);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(firstErrorField(response.getBody())).isEqualTo("color");
    }

    @Test
    void patchingChangesTheColourAndPatchingWithoutOneKeepsIt() {
        setUpStore();
        String tag = createTag("Хіт");
        String original = (String) getTag("products", tag).get("color");

        assertThat(patch("/sellers/tags/products/" + tag, """
                {"status":"INACTIVE"}""").getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(getTag("products", tag)).containsEntry("color", original);

        assertThat(patch("/sellers/tags/products/" + tag, """
                {"color":"#0091ff"}""").getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(getTag("products", tag)).containsEntry("color", "#0091FF");
    }

    @Test
    void aNameIsStoredTrimmedAndCollapsed() {
        setUpStore();

        assertThat(createTagResponse("products", "  Хіт   сезону ").getBody())
                .containsEntry("name", "Хіт сезону");
    }

    @Test
    void theSameNameCannotBeCreatedTwiceEvenInAnotherCase() {
        setUpStore();
        createTag("Хіт");

        ResponseEntity<Map<String, Object>> response = createTagResponse("products", "хіт");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(firstErrorField(response.getBody())).isEqualTo("name");
    }

    @Test
    void renamingOntoATakenNameIsRefusedRatherThanMerged() {
        setUpStore();
        createTag("Хіт");
        String sale = createTag("Розпродаж");

        ResponseEntity<Map<String, Object>> response = rest.exchange(
                url("/sellers/tags/products/" + sale), HttpMethod.PATCH,
                new HttpEntity<>("""
                        {"name":"ХІТ"}""", headers()), MAP);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat((String) response.getBody().get("detail")).contains("merge");
    }

    @Test
    void renamingKeepsWhatTheTagLabels() {
        setUpStore();
        String tag = createTag("Хіт");
        String product = createProduct("G-1");
        pin(product, tag);

        assertThat(patch("/sellers/tags/products/" + tag, """
                {"name":"Хіт сезону"}""").getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        assertThat(getTag("products", tag))
                .containsEntry("name", "Хіт сезону")
                .containsEntry("usage_count", 1);
    }

    @Test
    void theUsageCountShowsHowManyThingsTheTagLabels() {
        setUpStore();
        String tag = createTag("Хіт");
        pin(createProduct("G-2"), tag);
        pin(createProduct("G-3"), tag);

        assertThat(getTag("products", tag)).containsEntry("usage_count", 2);
        assertThat(dataOf(listTags("products", ""))).first()
                .satisfies(entry -> assertThat(entry).containsEntry("usage_count", 2));
    }

    /** A label the seller stopped using is meant to disappear; the things it labelled are not. */
    @Test
    void deletingATagUntagsItsProductsAndLeavesThemAlone() {
        setUpStore();
        String tag = createTag("Хіт");
        String product = createProduct("G-4");
        pin(product, tag);

        assertThat(exchange(HttpMethod.DELETE, "/sellers/tags/products/" + tag, null).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);

        assertThat(countTags()).isZero();
        assertThat(productTags(product)).isEmpty();
        assertThat(rest.exchange(url("/sellers/products/" + product), HttpMethod.GET,
                new HttpEntity<>(headers()), String.class).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void mergingMovesTheAssignmentsAndDropsTheSources() {
        setUpStore();
        String survivor = createTag("Хіт");
        String source = createTag("Хіт продажів");
        String onlySource = createProduct("G-5");
        String both = createProduct("G-6");
        pin(onlySource, source);
        pin(both, survivor);
        pin(both, source);

        ResponseEntity<Map<String, Object>> response = rest.exchange(
                url("/sellers/tags/products/" + survivor + "/merge"), HttpMethod.POST,
                new HttpEntity<>("""
                        {"source_ids":["%s"]}""".formatted(source), headers()), MAP);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("name", "Хіт").containsEntry("usage_count", 2);
        assertThat(countTags()).isEqualTo(1);
        // The product that carried both keeps one link, not two.
        assertThat(productTags(both)).hasSize(1);
        assertThat(productTags(onlySource)).extracting(tag -> tag.get("name")).containsExactly("Хіт");
    }

    @Test
    void aTagCannotBeMergedIntoItself() {
        setUpStore();
        String tag = createTag("Хіт");

        ResponseEntity<Map<String, Object>> response = rest.exchange(
                url("/sellers/tags/products/" + tag + "/merge"), HttpMethod.POST,
                new HttpEntity<>("""
                        {"source_ids":["%s"]}""".formatted(tag), headers()), MAP);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(firstErrorField(response.getBody())).isEqualTo("source_ids");
    }

    /** Products and orders keep separate vocabularies, and the target is what keeps them apart. */
    @Test
    void aProductTagIsInvisibleThroughTheOrderTarget() {
        setUpStore();
        String tag = createTag("Хіт");

        assertThat(rest.exchange(url("/sellers/tags/orders/" + tag), HttpMethod.GET,
                new HttpEntity<>(headers()), String.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exchange(HttpMethod.DELETE, "/sellers/tags/orders/" + tag, null).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(dataOf(listTags("orders", ""))).isEmpty();
    }

    @Test
    void theSameNameMayExistForBothTargets() {
        setUpStore();
        createTag("Терміново");

        assertThat(createTagResponse("orders", "Терміново").getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(countTags()).isEqualTo(2);
    }

    @Test
    void anUnknownTargetSegmentIsRejected() {
        setUpStore();

        assertThat(rest.exchange(url("/sellers/tags/customers"), HttpMethod.GET,
                new HttpEntity<>(headers()), String.class).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void theListingSearchesFiltersAndPages() {
        setUpStore();
        createTag("Хіт");
        createTag("Хіт сезону");
        String archived = createTag("Розпродаж");
        patch("/sellers/tags/products/" + archived, """
                {"status":"INACTIVE"}""");

        assertThat(dataOf(listTags("products", "?search=хіт")))
                .extracting(tag -> tag.get("name")).containsExactly("Хіт", "Хіт сезону");
        assertThat(dataOf(listTags("products", "?statuses=INACTIVE")))
                .extracting(tag -> tag.get("name")).containsExactly("Розпродаж");

        Map<String, Object> firstPage = listTags("products", "?limit=2");
        assertThat(dataOf(firstPage)).hasSize(2);
        assertThat(firstPage.get("count")).isEqualTo(3);
        assertThat(firstPage.get("pages")).isEqualTo(2);
        // Tags are read as a picker, so the default order is alphabetical rather than newest first.
        assertThat(dataOf(firstPage)).extracting(tag -> tag.get("name"))
                .containsExactly("Розпродаж", "Хіт");
    }

    @Test
    void anInactiveTagStillMatchesASubmittedName() {
        setUpStore();
        String tag = createTag("Хіт");
        patch("/sellers/tags/products/" + tag, """
                {"status":"INACTIVE"}""");
        String product = createProduct("G-7");

        assertThat(replace(product, """
                {"tags":[{"name":"хіт"}]}""").getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(countTags()).isEqualTo(1);
        assertThat(productTags(product)).extracting(entry -> entry.get("public_id")).containsExactly(tag);
    }

    // --- helpers -------------------------------------------------------------------------------

    private void setUpStore() {
        accountId = registerSeller();
        storeId = createStore();
    }

    private String registerSeller() {
        String body = "{\"keycloakUserId\":\"" + EXT_ID + "\",\"email\":\"dictionary@example.com\","
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

    private String createProduct(String code) {
        Long stockStatusId = jdbcTemplate.queryForObject(
                "SELECT id FROM stock_statuses ORDER BY id LIMIT 1", Long.class);
        String body = "{\"name\":\"Widget\",\"code\":\"" + code + "\",\"stock_status_id\":" + stockStatusId
                + ",\"price\":100,\"quantity\":1}";
        assertThat(exchange(HttpMethod.POST, "/sellers/products", body).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
        return jdbcTemplate.queryForObject(
                "SELECT public_id FROM store_products WHERE code = ?", String.class, code);
    }

    private ResponseEntity<Map<String, Object>> createTagResponse(String target, String name) {
        return rest.exchange(url("/sellers/tags/" + target), HttpMethod.POST,
                new HttpEntity<>("{\"name\":\"" + name + "\"}", headers()), MAP);
    }

    private String createTag(String name) {
        ResponseEntity<Map<String, Object>> response = createTagResponse("products", name);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return (String) response.getBody().get("public_id");
    }

    private Map<String, Object> getTag(String target, String publicId) {
        return rest.exchange(url("/sellers/tags/" + target + "/" + publicId), HttpMethod.GET,
                new HttpEntity<>(headers()), MAP).getBody();
    }

    private Map<String, Object> listTags(String target, String queryString) {
        return rest.exchange(url("/sellers/tags/" + target + queryString), HttpMethod.GET,
                new HttpEntity<>(headers()), MAP).getBody();
    }

    private void pin(String productPublicId, String tagPublicId) {
        assertThat(rest.exchange(url("/sellers/product-tags/bulk"), HttpMethod.POST,
                new HttpEntity<>("""
                        {"product_ids":["%s"],"tags":[{"id":"%s"}]}"""
                        .formatted(productPublicId, tagPublicId), headers()), MAP)
                .getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private List<Map<String, Object>> productTags(String productPublicId) {
        return rest.exchange(url("/sellers/products/" + productPublicId + "/tags"), HttpMethod.GET,
                new HttpEntity<>(headers()), LIST_OF_MAPS).getBody();
    }

    private ResponseEntity<String> replace(String productPublicId, String body) {
        return exchange(HttpMethod.PUT, "/sellers/products/" + productPublicId + "/tags", body);
    }

    private ResponseEntity<String> patch(String path, String body) {
        return exchange(HttpMethod.PATCH, path, body);
    }

    private int countTags() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM store_tags", Integer.class);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> dataOf(Map<String, Object> page) {
        return (List<Map<String, Object>>) page.get("data");
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
        headers.add(HttpConstants.REQUEST_ID_HEADER, "corr-tag-dictionary");
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
