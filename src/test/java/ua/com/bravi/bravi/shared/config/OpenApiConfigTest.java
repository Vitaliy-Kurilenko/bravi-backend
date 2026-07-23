package ua.com.bravi.bravi.shared.config;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.junit.jupiter.api.Test;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.web.method.HandlerMethod;
import ua.com.bravi.bravi.seller.controller.AccountController;
import ua.com.bravi.bravi.seller.controller.SellerProductController;
import ua.com.bravi.bravi.seller.controller.SellerStoresController;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the springdoc {@link OperationCustomizer} documents the context headers per endpoint:
 * X-Correlation-Id everywhere; X-Account-Id on {@code /sellers/**}; X-Store-Id only on store-scoped
 * sub-resources (not the path-based {@code /sellers/stores/{storeId}} resource, not discovery).
 */
class OpenApiConfigTest {

    private final OperationCustomizer customizer = new OpenApiConfig().contextHeadersCustomizer();

    @Test
    void storeScopedSubResourceGetsAllThreeHeaders() {
        Operation op = customize(SellerProductController.class, "searchProducts");
        assertThat(headerNames(op)).contains("X-Correlation-Id", "X-Account-Id", "X-Store-Id");
    }

    @Test
    void pathBasedStoreResourceHasNoStoreHeader() {
        Operation op = customize(SellerStoresController.class, "getStore");
        assertThat(headerNames(op)).contains("X-Correlation-Id", "X-Account-Id");
        assertThat(headerNames(op)).doesNotContain("X-Store-Id");
    }

    @Test
    void accountScopedListingHasNoStoreHeader() {
        Operation op = customize(SellerStoresController.class, "listStores");
        assertThat(headerNames(op)).contains("X-Correlation-Id", "X-Account-Id");
        assertThat(headerNames(op)).doesNotContain("X-Store-Id");
    }

    @Test
    void discoveryEndpointOnlyGetsCorrelationHeader() {
        Operation op = customize(AccountController.class, "getAccounts");
        assertThat(headerNames(op)).containsExactly("X-Correlation-Id");
    }

    private Operation customize(Class<?> controller, String methodName) {
        Operation operation = new Operation();
        customizer.customize(operation, handlerMethod(controller, methodName));
        return operation;
    }

    private static HandlerMethod handlerMethod(Class<?> controller, String methodName) {
        Object bean = instantiateWithNulls(controller);
        Method method = Arrays.stream(controller.getDeclaredMethods())
                .filter(m -> m.getName().equals(methodName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No method " + methodName));
        return new HandlerMethod(bean, method);
    }

    private static Object instantiateWithNulls(Class<?> controller) {
        var constructor = controller.getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        Object[] args = new Object[constructor.getParameterCount()];
        try {
            return constructor.newInstance(args);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static java.util.List<String> headerNames(Operation op) {
        if (op.getParameters() == null) {
            return java.util.List.of();
        }
        return op.getParameters().stream()
                .filter(p -> "header".equals(p.getIn()))
                .map(Parameter::getName)
                .toList();
    }
}
