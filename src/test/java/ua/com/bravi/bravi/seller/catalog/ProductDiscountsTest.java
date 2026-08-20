package ua.com.bravi.bravi.seller.catalog;

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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Walks the discount schedule against a real database: what the seller saves, what the product then
 * costs, and the rules that stop two discounts from ever running at once.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductDiscountsTest extends AbstractPostgresIT {

    private static final String SERVICE_TOKEN = "service";
    private static final String USER_TOKEN = "user";
    private static final UUID EXT_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final ParameterizedTypeReference<List<Map<String, Object>>> LIST_OF_MAPS =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<Map<String, Object>> MAP =
            new ParameterizedTypeReference<>() {
            };

    private static final Instant NOW = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    private static final String RUNNING_FROM = NOW.minus(1, ChronoUnit.DAYS).toString();
    private static final String RUNNING_TO = NOW.plus(3, ChronoUnit.DAYS).toString();
    private static final String LATER_FROM = NOW.plus(10, ChronoUnit.DAYS).toString();
    private static final String LATER_TO = NOW.plus(20, ChronoUnit.DAYS).toString();

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
                    .claim("preferred_username", "discounts@example.com")
                    .claim("email", "discounts@example.com")
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
    void aRunningDiscountShapesTheProductPriceAndAScheduledOneDoesNot() {
        setUpStore();
        String product = createProduct("D-1", "1200.00");

        List<Map<String, Object>> saved = replaceDiscounts(product, """
                {"discounts":[
                  {"type":"PERCENT","value":20,"starts_at":"%s","ends_at":"%s","label":"Чорна п'ятниця"},
                  {"type":"AMOUNT","value":150,"starts_at":"%s","ends_at":"%s"}
                ]}""".formatted(RUNNING_FROM, RUNNING_TO, LATER_FROM, LATER_TO));

        assertThat(saved).hasSize(2);
        assertThat(saved).extracting(entry -> entry.get("status"))
                .containsExactly("ACTIVE", "SCHEDULED");
        assertThat(saved).allSatisfy(entry ->
                assertThat((String) entry.get("public_id")).startsWith("dsc_"));

        Map<String, Object> view = getProduct(product);
        assertThat(view.get("discounted_price")).isEqualTo(960.00);

        @SuppressWarnings("unchecked")
        Map<String, Object> active = (Map<String, Object>) view.get("active_discount");
        assertThat(active).containsEntry("type", "PERCENT").containsEntry("label", "Чорна п'ятниця");
        // The product payload answers "what does it cost and until when", nothing more.
        assertThat(active).containsOnlyKeys("public_id", "type", "value", "label", "ends_at");
    }

    @Test
    void aProductWithoutARunningDiscountReportsNoDiscountedPrice() {
        setUpStore();
        String product = createProduct("D-2", "1200.00");
        replaceDiscounts(product, """
                {"discounts":[{"type":"PERCENT","value":20,"starts_at":"%s","ends_at":"%s"}]}"""
                .formatted(LATER_FROM, LATER_TO));

        Map<String, Object> view = getProduct(product);
        assertThat(view.get("discounted_price")).isNull();
        assertThat(view.get("active_discount")).isNull();
    }

    @Test
    void resubmittingAScheduleKeepsIdentityAndDroppingAnEntryDeletesIt() {
        setUpStore();
        String product = createProduct("D-3", "1200.00");
        List<Map<String, Object>> saved = replaceDiscounts(product, """
                {"discounts":[
                  {"type":"PERCENT","value":20,"starts_at":"%s","ends_at":"%s"},
                  {"type":"AMOUNT","value":150,"starts_at":"%s","ends_at":"%s"}
                ]}""".formatted(RUNNING_FROM, RUNNING_TO, LATER_FROM, LATER_TO));
        String keptId = (String) saved.getFirst().get("public_id");
        String createdAt = jdbcTemplate.queryForObject(
                "SELECT created_at::text FROM store_product_discounts WHERE public_id = ?", String.class, keptId);

        List<Map<String, Object>> after = replaceDiscounts(product, """
                {"discounts":[{"public_id":"%s","type":"PERCENT","value":25,
                  "starts_at":"%s","ends_at":"%s"}]}"""
                .formatted(keptId, RUNNING_FROM, RUNNING_TO));

        assertThat(after).singleElement()
                .satisfies(entry -> assertThat(entry.get("public_id")).isEqualTo(keptId));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT created_at::text FROM store_product_discounts WHERE public_id = ?", String.class, keptId))
                .isEqualTo(createdAt);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM store_product_discounts", Integer.class)).isEqualTo(1);
        assertThat(getProduct(product).get("discounted_price")).isEqualTo(900.00);
    }

    @Test
    void aWholeScheduleCanBeClearedWhichStopsARunningDiscount() {
        setUpStore();
        String product = createProduct("D-4", "1200.00");
        replaceDiscounts(product, """
                {"discounts":[{"type":"PERCENT","value":20,"starts_at":"%s","ends_at":"%s"}]}"""
                .formatted(RUNNING_FROM, RUNNING_TO));

        assertThat(replaceDiscounts(product, "{\"discounts\":[]}")).isEmpty();
        assertThat(getProduct(product).get("discounted_price")).isNull();
    }

    @Test
    void overlappingPeriodsAreRejectedAndNameTheDiscountTheyCollideWith() {
        setUpStore();
        String product = createProduct("D-5", "1200.00");

        ResponseEntity<String> response = exchange(HttpMethod.PUT,
                "/sellers/products/" + product + "/discounts", """
                        {"discounts":[
                          {"type":"PERCENT","value":20,"starts_at":"%s","ends_at":"%s","label":"Перша"},
                          {"type":"PERCENT","value":10,"starts_at":"%s","ends_at":"%s"}
                        ]}""".formatted(RUNNING_FROM, LATER_TO, LATER_FROM, LATER_TO));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).contains("Discount periods overlap").contains("discounts[1].starts_at");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM store_product_discounts", Integer.class)).isZero();
    }

    @Test
    void anOpenEndedDiscountCannotPrecedeAScheduledOne() {
        setUpStore();
        String product = createProduct("D-6", "1200.00");

        ResponseEntity<String> response = exchange(HttpMethod.PUT,
                "/sellers/products/" + product + "/discounts", """
                        {"discounts":[
                          {"type":"PERCENT","value":5,"starts_at":"%s","ends_at":null},
                          {"type":"PERCENT","value":10,"starts_at":"%s","ends_at":"%s"}
                        ]}""".formatted(RUNNING_FROM, LATER_FROM, LATER_TO));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void anOpenEndedDiscountRunsUntilItIsRemoved() {
        setUpStore();
        String product = createProduct("D-7", "1200.00");

        List<Map<String, Object>> saved = replaceDiscounts(product, """
                {"discounts":[{"type":"PERCENT","value":5,"starts_at":"%s","ends_at":null}]}"""
                .formatted(RUNNING_FROM));

        assertThat(saved).singleElement()
                .satisfies(entry -> assertThat(entry.get("status")).isEqualTo("ACTIVE"));
        assertThat(saved.getFirst().get("ends_at")).isNull();
        assertThat(getProduct(product).get("discounted_price")).isEqualTo(1140.00);
    }

    @Test
    void theStartOfARunningDiscountCannotBeMoved() {
        setUpStore();
        String product = createProduct("D-8", "1200.00");
        String publicId = (String) replaceDiscounts(product, """
                {"discounts":[{"type":"PERCENT","value":20,"starts_at":"%s","ends_at":"%s"}]}"""
                .formatted(RUNNING_FROM, RUNNING_TO)).getFirst().get("public_id");

        ResponseEntity<String> response = exchange(HttpMethod.PUT,
                "/sellers/products/" + product + "/discounts", """
                        {"discounts":[{"public_id":"%s","type":"PERCENT","value":20,
                          "starts_at":"%s","ends_at":"%s"}]}"""
                .formatted(publicId, NOW.minus(2, ChronoUnit.DAYS), RUNNING_TO));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("discounts[0].starts_at");
    }

    @Test
    void invalidValuesAreRejectedOnTheEntryThatCarriesThem() {
        setUpStore();
        String product = createProduct("D-9", "1200.00");

        ResponseEntity<String> percent = exchange(HttpMethod.PUT,
                "/sellers/products/" + product + "/discounts", """
                        {"discounts":[{"type":"PERCENT","value":150,"starts_at":"%s","ends_at":"%s"}]}"""
                .formatted(RUNNING_FROM, RUNNING_TO));
        assertThat(percent.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(percent.getBody()).contains("discounts[0].value")
                .contains("Percent discount must be between 0.01 and 99");

        ResponseEntity<String> amount = exchange(HttpMethod.PUT,
                "/sellers/products/" + product + "/discounts", """
                        {"discounts":[{"type":"AMOUNT","value":1200,"starts_at":"%s","ends_at":"%s"}]}"""
                .formatted(RUNNING_FROM, RUNNING_TO));
        assertThat(amount.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(amount.getBody()).contains("larger than the product price");

        ResponseEntity<String> period = exchange(HttpMethod.PUT,
                "/sellers/products/" + product + "/discounts", """
                        {"discounts":[{"type":"PERCENT","value":20,"starts_at":"%s","ends_at":"%s"}]}"""
                .formatted(RUNNING_TO, RUNNING_FROM));
        assertThat(period.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(period.getBody()).contains("discounts[0].ends_at");
    }

    @Test
    void aPriceCutBelowALiveAmountDiscountIsRejectedAndLeavesThePriceAlone() {
        setUpStore();
        String product = createProduct("D-10", "1200.00");
        replaceDiscounts(product, """
                {"discounts":[{"type":"AMOUNT","value":500,"starts_at":"%s","ends_at":"%s"}]}"""
                .formatted(RUNNING_FROM, RUNNING_TO));

        ResponseEntity<String> response = exchange(HttpMethod.PATCH,
                "/sellers/products/" + product, "{\"price\":400.00}");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("\"field\":\"price\"");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT price FROM store_products WHERE public_id = ?", java.math.BigDecimal.class, product))
                .isEqualByComparingTo("1200.00");
    }

    @Test
    void theSearchFilterPartitionsDiscountedProductsAndCountsThemCorrectly() {
        setUpStore();
        String discounted = createProduct("D-11", "1200.00");
        createProduct("D-12", "500.00");
        replaceDiscounts(discounted, """
                {"discounts":[{"type":"PERCENT","value":20,"starts_at":"%s","ends_at":"%s"}]}"""
                .formatted(RUNNING_FROM, RUNNING_TO));

        Map<String, Object> withDiscount = searchProducts("?has_active_discount=true");
        assertThat(withDiscount.get("count")).isEqualTo(1);
        assertThat(dataOf(withDiscount)).singleElement()
                .satisfies(entry -> assertThat(entry.get("public_id")).isEqualTo(discounted));

        Map<String, Object> withoutDiscount = searchProducts("?has_active_discount=false");
        assertThat(withoutDiscount.get("count")).isEqualTo(1);
        assertThat(dataOf(withoutDiscount)).singleElement()
                .satisfies(entry -> assertThat(entry.get("code")).isEqualTo("D-12"));

        assertThat(searchProducts("").get("count")).isEqualTo(2);
    }

    @Test
    void theProductListCarriesTheDiscountedPrice() {
        setUpStore();
        String product = createProduct("D-13", "1200.00");
        replaceDiscounts(product, """
                {"discounts":[{"type":"PERCENT","value":20,"starts_at":"%s","ends_at":"%s"}]}"""
                .formatted(RUNNING_FROM, RUNNING_TO));

        assertThat(dataOf(searchProducts(""))).singleElement()
                .satisfies(entry -> assertThat(entry.get("discounted_price")).isEqualTo(960.00));
    }

    @Test
    void bulkAppliesToEveryProductItCanAndReportsTheRest() {
        setUpStore();
        String free = createProduct("D-14", "1200.00");
        String taken = createProduct("D-15", "1200.00");
        String cheap = createProduct("D-16", "50.00");
        replaceDiscounts(taken, """
                {"discounts":[{"type":"PERCENT","value":20,"starts_at":"%s","ends_at":"%s"}]}"""
                .formatted(RUNNING_FROM, RUNNING_TO));

        ResponseEntity<Map<String, Object>> response = rest.exchange(
                url("/sellers/product-discounts/bulk"), HttpMethod.POST,
                new HttpEntity<>("""
                        {"product_ids":["%s","%s","%s"],"type":"AMOUNT","value":100,
                         "starts_at":"%s","ends_at":"%s"}"""
                        .formatted(free, taken, cheap, RUNNING_FROM, RUNNING_TO), headers()), MAP);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("applied", 1);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> skipped = (List<Map<String, Object>>) response.getBody().get("skipped");
        assertThat(skipped).hasSize(2)
                .anySatisfy(entry -> {
                    assertThat(entry.get("product_id")).isEqualTo(taken);
                    assertThat(entry.get("reason")).isEqualTo("PERIOD_OVERLAP");
                })
                .anySatisfy(entry -> {
                    assertThat(entry.get("product_id")).isEqualTo(cheap);
                    assertThat(entry.get("reason")).isEqualTo("AMOUNT_EXCEEDS_PRICE");
                });
        assertThat(getProduct(free).get("discounted_price")).isEqualTo(1100.00);
    }

    @Test
    void aBulkRequestWithAnInvalidValueFailsAsAWholeAndNamesItsOwnField() {
        setUpStore();
        String product = createProduct("D-19", "1200.00");

        ResponseEntity<String> response = rest.exchange(
                url("/sellers/product-discounts/bulk"), HttpMethod.POST,
                new HttpEntity<>("""
                        {"product_ids":["%s"],"type":"PERCENT","value":150,
                         "starts_at":"%s","ends_at":"%s"}"""
                        .formatted(product, RUNNING_FROM, RUNNING_TO), headers()), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        // A flat request must not report an array position it never had.
        assertThat(response.getBody()).contains("\"field\":\"value\"").doesNotContain("discounts[");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM store_product_discounts", Integer.class)).isZero();
    }

    @Test
    void aBulkRequestWithABackwardsPeriodNamesItsOwnField() {
        setUpStore();
        String product = createProduct("D-20", "1200.00");

        ResponseEntity<String> response = rest.exchange(
                url("/sellers/product-discounts/bulk"), HttpMethod.POST,
                new HttpEntity<>("""
                        {"product_ids":["%s"],"type":"PERCENT","value":20,
                         "starts_at":"%s","ends_at":"%s"}"""
                        .formatted(product, RUNNING_TO, RUNNING_FROM), headers()), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("\"field\":\"ends_at\"").doesNotContain("discounts[");
    }

    @Test
    void aDiscountOfAnotherStoresProductIsNotFound() {
        setUpStore();

        assertThat(exchange(HttpMethod.GET, "/sellers/products/prd_missing/discounts", null).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void anUnknownDiscountInTheBodyIsRejected() {
        setUpStore();
        String product = createProduct("D-17", "1200.00");

        ResponseEntity<String> response = exchange(HttpMethod.PUT,
                "/sellers/products/" + product + "/discounts", """
                        {"discounts":[{"public_id":"dsc_nope","type":"PERCENT","value":20,
                          "starts_at":"%s","ends_at":"%s"}]}""".formatted(RUNNING_FROM, RUNNING_TO));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void aScheduleReadBackMayBeSentStraightBack() {
        setUpStore();
        String product = createProduct("D-18", "1200.00");
        replaceDiscounts(product, """
                {"discounts":[{"type":"PERCENT","value":20,"starts_at":"%s","ends_at":"%s"}]}"""
                .formatted(RUNNING_FROM, RUNNING_TO));

        List<Map<String, Object>> read = rest.exchange(
                url("/sellers/products/" + product + "/discounts"), HttpMethod.GET,
                new HttpEntity<>(headers()), LIST_OF_MAPS).getBody();
        Map<String, Object> entry = read.getFirst();

        // The response carries derived fields the request does not declare; they must be ignored, not rejected.
        String echoed = """
                {"discounts":[{"public_id":"%s","type":"%s","value":%s,"starts_at":"%s","ends_at":"%s",
                  "label":null,"status":"%s","created_at":"%s","updated_at":null}]}"""
                .formatted(entry.get("public_id"), entry.get("type"), entry.get("value"),
                        entry.get("starts_at"), entry.get("ends_at"), entry.get("status"), entry.get("created_at"));

        assertThat(exchange(HttpMethod.PUT, "/sellers/products/" + product + "/discounts", echoed)
                .getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // --- helpers -------------------------------------------------------------------------------

    private void setUpStore() {
        accountId = registerSeller();
        storeId = createStore();
    }

    private String registerSeller() {
        String body = "{\"keycloakUserId\":\"" + EXT_ID + "\",\"email\":\"discounts@example.com\","
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

    private String createProduct(String code, String price) {
        Long stockStatusId = jdbcTemplate.queryForObject(
                "SELECT id FROM stock_statuses ORDER BY id LIMIT 1", Long.class);
        String body = "{\"name\":\"Widget\",\"code\":\"" + code + "\",\"stock_status_id\":" + stockStatusId
                + ",\"price\":" + price + ",\"quantity\":1}";
        assertThat(exchange(HttpMethod.POST, "/sellers/products", body).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
        return jdbcTemplate.queryForObject(
                "SELECT public_id FROM store_products WHERE code = ?", String.class, code);
    }

    private List<Map<String, Object>> replaceDiscounts(String productPublicId, String body) {
        ResponseEntity<List<Map<String, Object>>> response = rest.exchange(
                url("/sellers/products/" + productPublicId + "/discounts"), HttpMethod.PUT,
                new HttpEntity<>(body, headers()), LIST_OF_MAPS);
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

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> dataOf(Map<String, Object> page) {
        return (List<Map<String, Object>>) page.get("data");
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
        headers.add(HttpConstants.REQUEST_ID_HEADER, "corr-discounts");
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
