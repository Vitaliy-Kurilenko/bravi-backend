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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Walks the whole feature against a real database: adopting library templates into a store, letting a
 * subcategory inherit them, filling values on a product, and the rules that guard all of it.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductAttributesTest extends AbstractPostgresIT {

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
                    .subject(UUID.fromString("99999999-9999-9999-9999-999999999999").toString())
                    .claim("resource_access", Map.of("backend-service", Map.of("roles", List.of("auth_service"))))
                    .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300)).build();
            Jwt user = Jwt.withTokenValue(USER_TOKEN).header("alg", "none")
                    .subject(EXT_ID.toString())
                    .claim("preferred_username", "attributes@example.com")
                    .claim("email", "attributes@example.com")
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
    void templatesAreAdoptedOnBindingAndInheritedByEverySubcategory() {
        setUpStore();
        String clothing = createCategory("Одяг", null);
        String outerwear = createCategory("Верхній одяг", clothing);
        String jackets = createCategory("Куртки", outerwear);

        List<Map<String, Object>> bound = bindAttributes(clothing,
                "{\"template_codes\":[\"COLOR\",\"MATERIAL\"]}");

        assertThat(codesOf(bound)).containsExactly("COLOR", "MATERIAL");
        assertThat(jdbcTemplate.queryForList(
                "SELECT code FROM store_attributes ORDER BY code", String.class))
                .containsExactly("COLOR", "MATERIAL");
        // The library option list is copied along with the definition.
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM store_attribute_options o JOIN store_attributes a ON a.id = o.attribute_id "
                        + "WHERE a.code = 'COLOR'", Integer.class)).isEqualTo(10);

        List<Map<String, Object>> inherited = listCategoryAttributes(jackets);
        assertThat(codesOf(inherited)).containsExactly("COLOR", "MATERIAL");
        assertThat(inherited).allSatisfy(entry -> {
            assertThat(entry.get("source")).isEqualTo("INHERITED");
            assertThat(entry.get("source_category_name")).isEqualTo("Одяг");
        });

        // Binding on the leaf itself is reported as its own, and adopting an owned template does not duplicate it.
        bindAttributes(jackets, "{\"template_codes\":[\"SEASON\",\"COLOR\"]}");
        List<Map<String, Object>> mixed = listCategoryAttributes(jackets);
        assertThat(codesOf(mixed)).containsExactly("COLOR", "MATERIAL", "SEASON");
        assertThat(sourceOf(mixed, "SEASON")).isEqualTo("OWN");
        assertThat(sourceOf(mixed, "COLOR")).isEqualTo("INHERITED");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM store_attributes WHERE code = 'COLOR'", Integer.class)).isEqualTo(1);

        // Nothing was materialised, so moving the subtree under another root recomputes the set
        // with no data migration.
        String accessories = createCategory("Аксесуари", null);
        assertThat(exchange(HttpMethod.PATCH, "/sellers/categories/" + outerwear,
                "{\"parent_public_id\":\"" + accessories + "\"}").getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(codesOf(listCategoryAttributes(jackets))).containsExactly("SEASON");
    }

    @Test
    void aGlobalAttributeReachesEveryProductWithoutAnyBinding() {
        setUpStore();
        String category = createCategory("Взуття", null);
        createAttribute("{\"code\":\"WARRANTY\",\"name\":\"Гарантія\",\"value_type\":\"NUMBER\","
                + "\"scope\":\"GLOBAL\"}");

        List<Map<String, Object>> offered = listCategoryAttributes(category);
        assertThat(codesOf(offered)).containsExactly("WARRANTY");
        assertThat(offered.getFirst().get("source")).isEqualTo("GLOBAL");
        assertThat(offered.getFirst().get("source_category_id")).isNull();

        // A product without a category can still carry it.
        String productId = createProduct("CODE-1", null);
        ResponseEntity<String> filled = exchange(HttpMethod.PUT, "/sellers/products/" + productId + "/attributes",
                "{\"attributes\":[{\"attribute_id\":\"" + attributePublicId("WARRANTY") + "\","
                        + "\"value_number\":24}]}");
        assertThat(filled.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(filled.getBody()).contains("\"value_number\":24");

        // A global attribute needs no binding, so binding it is rejected rather than silently ignored.
        ResponseEntity<String> rejected = exchange(HttpMethod.POST,
                "/sellers/categories/" + category + "/attributes",
                "{\"attribute_ids\":[\"" + attributePublicId("WARRANTY") + "\"]}");
        assertThat(rejected.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(rejected.getBody()).contains("already reaches every product");
    }

    @Test
    void productValuesAreValidatedAgainstTheirDefinition() {
        setUpStore();
        String category = createCategory("Одяг", null);
        bindAttributes(category, "{\"template_codes\":[\"COLOR\",\"MATERIAL\",\"WEIGHT_NET\"]}");
        String productId = createProduct("CODE-1", category);

        String colorId = attributePublicId("COLOR");
        String materialId = attributePublicId("MATERIAL");
        String weightId = attributePublicId("WEIGHT_NET");
        String black = optionPublicId("COLOR", "BLACK");
        String cotton = optionPublicId("MATERIAL", "COTTON");
        String wool = optionPublicId("MATERIAL", "WOOL");

        ResponseEntity<String> saved = exchange(HttpMethod.PUT, "/sellers/products/" + productId + "/attributes",
                "{\"attributes\":["
                        + "{\"attribute_id\":\"" + colorId + "\",\"option_ids\":[\"" + black + "\"]},"
                        + "{\"attribute_id\":\"" + materialId + "\",\"option_ids\":[\"" + cotton + "\",\"" + wool + "\"]},"
                        + "{\"attribute_id\":\"" + weightId + "\",\"value_number\":1.5,\"unit_code\":\"KG\"}]}");
        assertThat(saved.getStatusCode()).isEqualTo(HttpStatus.OK);

        // A multi-select keeps one row per option; the others keep exactly one.
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM store_product_attribute_values", Integer.class)).isEqualTo(4);

        ResponseEntity<Map<String, Object>> product = rest.exchange(
                url("/sellers/products/" + productId), HttpMethod.GET, new HttpEntity<>(headers()), MAP);
        assertThat(codesOf(attributesOf(product.getBody()))).containsExactly("COLOR", "MATERIAL", "WEIGHT_NET");
        assertThat(attributesOf(product.getBody())).allSatisfy(value ->
                assertThat(value.get("offered")).isEqualTo(true));

        assertRejected(productId, colorId, "\"value_string\":\"Red\"", "takes options, not a literal value");
        assertRejected(productId, colorId, "\"option_ids\":[\"" + cotton + "\"]", "does not belong to attribute");
        assertRejected(productId, colorId, "\"option_ids\":[\"" + black + "\",\"" + wool + "\"]",
                "Exactly one option must be selected");
        assertRejected(productId, weightId, "\"value_number\":1.5,\"unit_code\":\"NOPE\"", "Unknown unit 'NOPE'");
        assertRejected(productId, weightId, "\"value_string\":\"heavy\"", "A numeric value is required");
    }

    @Test
    void anAttributeCarryingValuesCannotBeDeletedAndUnbindingKeepsTheValues() {
        setUpStore();
        String category = createCategory("Одяг", null);
        bindAttributes(category, "{\"template_codes\":[\"COLOR\"]}");
        String productId = createProduct("CODE-1", category);
        String colorId = attributePublicId("COLOR");

        assertThat(exchange(HttpMethod.PUT, "/sellers/products/" + productId + "/attributes",
                "{\"attributes\":[{\"attribute_id\":\"" + colorId + "\",\"option_ids\":[\""
                        + optionPublicId("COLOR", "BLACK") + "\"]}]}").getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> deleted = exchange(HttpMethod.DELETE, "/sellers/attributes/" + colorId, null);
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(deleted.getBody()).contains("used by products");

        // Unbinding stops the offer but keeps what products already carry, reported as no longer offered.
        assertThat(exchange(HttpMethod.DELETE, "/sellers/categories/" + category + "/attributes/" + colorId, null)
                .getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(listCategoryAttributes(category)).isEmpty();

        ResponseEntity<Map<String, Object>> attributes = rest.exchange(
                url("/sellers/products/" + productId + "/attributes"), HttpMethod.GET,
                new HttpEntity<>(headers()), MAP);
        List<Map<String, Object>> values = valuesOf(attributes.getBody());
        assertThat(values).singleElement().satisfies(value -> {
            assertThat(value.get("code")).isEqualTo("COLOR");
            assertThat(value.get("offered")).isEqualTo(false);
        });
    }

    @Test
    void anInheritedBindingIsChangedOnTheAncestorThatDefinesIt() {
        setUpStore();
        String clothing = createCategory("Одяг", null);
        String jackets = createCategory("Куртки", clothing);
        bindAttributes(clothing, "{\"template_codes\":[\"COLOR\"]}");

        ResponseEntity<String> response = exchange(HttpMethod.DELETE,
                "/sellers/categories/" + jackets + "/attributes/" + attributePublicId("COLOR"), null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("inherited from a parent category");
        assertThat(codesOf(listCategoryAttributes(jackets))).containsExactly("COLOR");
    }

    @Test
    void valuesAreCopiedFromAnotherProductAndAppliedToManyAtOnce() {
        setUpStore();
        String category = createCategory("Одяг", null);
        bindAttributes(category, "{\"template_codes\":[\"COLOR\",\"SEASON\"]}");
        String colorId = attributePublicId("COLOR");
        String seasonId = attributePublicId("SEASON");
        String black = optionPublicId("COLOR", "BLACK");

        String source = createProduct("CODE-1", category);
        String target = createProduct("CODE-2", category);
        String third = createProduct("CODE-3", category);

        assertThat(exchange(HttpMethod.PUT, "/sellers/products/" + source + "/attributes",
                "{\"attributes\":[{\"attribute_id\":\"" + colorId + "\",\"option_ids\":[\"" + black + "\"]}]}")
                .getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> copied = exchange(HttpMethod.POST,
                "/sellers/products/" + target + "/attributes/copy-from", "{\"product_id\":\"" + source + "\"}");
        assertThat(copied.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(copied.getBody()).contains("\"code\":\"COLOR\"").contains("\"code\":\"BLACK\"");

        ResponseEntity<String> bulk = exchange(HttpMethod.POST, "/sellers/product-attributes/bulk",
                "{\"product_ids\":[\"" + source + "\",\"" + target + "\",\"" + third + "\"],"
                        + "\"attributes\":[{\"attribute_id\":\"" + seasonId + "\",\"option_ids\":[\""
                        + optionPublicId("SEASON", "WINTER") + "\"]}]}");
        assertThat(bulk.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(bulk.getBody()).contains("\"updated\":3");

        // Bulk merges: the colour set earlier on two of the products survives.
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM store_product_attribute_values v JOIN store_attributes a "
                        + "ON a.id = v.attribute_id WHERE a.code = 'SEASON'", Integer.class)).isEqualTo(3);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM store_product_attribute_values v JOIN store_attributes a "
                        + "ON a.id = v.attribute_id WHERE a.code = 'COLOR'", Integer.class)).isEqualTo(2);
    }

    @Test
    void aProductIsCreatedWithItsAttributesInOneRequest() {
        setUpStore();
        String category = createCategory("Одяг", null);
        bindAttributes(category, "{\"template_codes\":[\"COLOR\"]}");

        Long stockStatusId = jdbcTemplate.queryForObject(
                "SELECT id FROM stock_statuses ORDER BY id LIMIT 1", Long.class);
        String body = "{\"name\":\"Куртка\",\"code\":\"CODE-9\",\"stock_status_id\":" + stockStatusId
                + ",\"price\":10.00,\"quantity\":1,\"category_id\":\"" + category + "\","
                + "\"attributes\":[{\"attribute_id\":\"" + attributePublicId("COLOR") + "\",\"option_ids\":[\""
                + optionPublicId("COLOR", "BLACK") + "\"]}]}";

        ResponseEntity<String> created = exchange(HttpMethod.POST, "/sellers/products", body);

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM store_product_attribute_values", Integer.class)).isEqualTo(1);
    }

    @Test
    void theLibraryReportsWhichTemplatesTheStoreAlreadyOwns() {
        setUpStore();
        String category = createCategory("Одяг", null);
        bindAttributes(category, "{\"template_codes\":[\"COLOR\"]}");

        ResponseEntity<List<Map<String, Object>>> templates = rest.exchange(
                url("/sellers/attribute-templates"), HttpMethod.GET, new HttpEntity<>(headers()), LIST_OF_MAPS);

        assertThat(templates.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(templates.getBody()).hasSize(10);
        assertThat(templates.getBody()).filteredOn(template -> "COLOR".equals(template.get("code")))
                .singleElement()
                .satisfies(template -> {
                    assertThat(template.get("adopted")).isEqualTo(true);
                    assertThat(template.get("variant_defining")).isEqualTo(true);
                });
        assertThat(templates.getBody()).filteredOn(template -> "SEASON".equals(template.get("code")))
                .singleElement()
                .satisfies(template -> assertThat(template.get("adopted")).isEqualTo(false));
    }

    // ------------------------------------------------------------------ setup

    private void setUpStore() {
        accountId = registerSeller();
        storeId = createStore();
    }

    private void assertRejected(String productId, String attributeId, String valueJson, String message) {
        ResponseEntity<String> response = exchange(HttpMethod.PUT, "/sellers/products/" + productId + "/attributes",
                "{\"attributes\":[{\"attribute_id\":\"" + attributeId + "\"," + valueJson + "}]}");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains(message).contains("\"field\":\"attributes.");
    }

    private String createCategory(String name, String parentPublicId) {
        String body = "{\"name\":\"" + name + "\""
                + (parentPublicId == null ? "" : ",\"parent_public_id\":\"" + parentPublicId + "\"") + "}";
        ResponseEntity<String> response = exchange(HttpMethod.POST, "/sellers/categories", body);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return jdbcTemplate.queryForObject(
                "SELECT public_id FROM store_categories WHERE name = ?", String.class, name);
    }

    private String createAttribute(String body) {
        ResponseEntity<String> response = exchange(HttpMethod.POST, "/sellers/attributes", body);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    private List<Map<String, Object>> bindAttributes(String categoryPublicId, String body) {
        ResponseEntity<List<Map<String, Object>>> response = rest.exchange(
                url("/sellers/categories/" + categoryPublicId + "/attributes"), HttpMethod.POST,
                new HttpEntity<>(body, headers()), LIST_OF_MAPS);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    private List<Map<String, Object>> listCategoryAttributes(String categoryPublicId) {
        ResponseEntity<List<Map<String, Object>>> response = rest.exchange(
                url("/sellers/categories/" + categoryPublicId + "/attributes"), HttpMethod.GET,
                new HttpEntity<>(headers()), LIST_OF_MAPS);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    private String createProduct(String code, String categoryPublicId) {
        Long stockStatusId = jdbcTemplate.queryForObject(
                "SELECT id FROM stock_statuses ORDER BY id LIMIT 1", Long.class);
        String body = "{\"name\":\"Widget\",\"code\":\"" + code + "\",\"stock_status_id\":" + stockStatusId
                + ",\"price\":10.00,\"quantity\":1"
                + (categoryPublicId == null ? "" : ",\"category_id\":\"" + categoryPublicId + "\"") + "}";
        assertThat(exchange(HttpMethod.POST, "/sellers/products", body).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
        return jdbcTemplate.queryForObject(
                "SELECT public_id FROM store_products WHERE code = ?", String.class, code);
    }

    private String attributePublicId(String code) {
        return jdbcTemplate.queryForObject(
                "SELECT public_id FROM store_attributes WHERE code = ?", String.class, code);
    }

    private String optionPublicId(String attributeCode, String optionCode) {
        return jdbcTemplate.queryForObject(
                "SELECT o.public_id FROM store_attribute_options o JOIN store_attributes a ON a.id = o.attribute_id "
                        + "WHERE a.code = ? AND o.code = ?", String.class, attributeCode, optionCode);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> attributesOf(Map<String, Object> product) {
        return (List<Map<String, Object>>) product.get("attributes");
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> valuesOf(Map<String, Object> productAttributes) {
        return (List<Map<String, Object>>) productAttributes.get("values");
    }

    @SuppressWarnings("unchecked")
    private static List<Object> codesOf(List<Map<String, Object>> entries) {
        return entries.stream()
                .map(entry -> entry.containsKey("attribute")
                        ? ((Map<String, Object>) entry.get("attribute")).get("code")
                        : entry.get("code"))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private static Object sourceOf(List<Map<String, Object>> entries, String code) {
        return entries.stream()
                .filter(entry -> code.equals(((Map<String, Object>) entry.get("attribute")).get("code")))
                .findFirst()
                .orElseThrow()
                .get("source");
    }

    private String registerSeller() {
        String body = "{\"keycloakUserId\":\"" + EXT_ID + "\",\"email\":\"attributes@example.com\","
                + "\"firstName\":\"First\",\"lastName\":\"Last\"}";
        ResponseEntity<String> response = rest.exchange(url("/internal/registrations/seller"), HttpMethod.POST,
                new HttpEntity<>(body, headers(SERVICE_TOKEN, null, null)), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
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

    private ResponseEntity<String> exchange(HttpMethod method, String path, String body) {
        return rest.exchange(url(path), method, new HttpEntity<>(body, headers()), String.class);
    }

    private String url(String path) {
        return "http://localhost:" + port + "/api" + path;
    }

    private HttpHeaders headers() {
        return headers(USER_TOKEN, accountId, storeId);
    }

    private static HttpHeaders headers(String token, String accountId, String storeId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HttpConstants.REQUEST_ID_HEADER, "corr-attributes");
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
