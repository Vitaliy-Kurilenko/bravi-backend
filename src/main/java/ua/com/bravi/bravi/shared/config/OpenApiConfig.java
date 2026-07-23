package ua.com.bravi.bravi.shared.config;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.HandlerMethod;
import ua.com.bravi.bravi.shared.common.HttpConstants;
import ua.com.bravi.bravi.shared.component.RequireStore;

/**
 * Documents the context HTTP headers in Swagger. They are consumed by filters/interceptors
 * ({@code request.getHeader(...)}), not declared as controller parameters, so springdoc would not
 * otherwise show them. Applied per operation:
 * <ul>
 *   <li>{@code X-Correlation-Id} — required on every request (RequiredHeadersFilter);</li>
 *   <li>{@code X-Account-Id} — required on the seller surface ({@code /sellers/**});</li>
 *   <li>{@code X-Store-Id} — required on store-scoped sub-resources ({@code @RequireStore}); the store
 *       resource itself takes {@code {storeId}} in the path, so it is excluded.</li>
 * </ul>
 */
@Configuration
public class OpenApiConfig {

    private static final String HEADER_IN = "header";
    private static final String STORE_ID_PATH_TEMPLATE = "{storeId}";

    @Bean
    public OperationCustomizer contextHeadersCustomizer() {
        return (operation, handlerMethod) -> {
            addHeader(operation, HttpConstants.REQUEST_ID_HEADER,
                    "Correlation id; echoed back in the response.");

            String fullPath = fullPath(handlerMethod);
            if (fullPath.startsWith("/sellers")) {
                addHeader(operation, HttpConstants.ACCOUNT_ID_HEADER,
                        "Public id of the account in scope.");
                if (requiresStoreHeader(handlerMethod, fullPath)) {
                    addHeader(operation, HttpConstants.STORE_ID_HEADER,
                            "Public id of the store in scope.");
                }
            }
            return operation;
        };
    }

    private static boolean requiresStoreHeader(HandlerMethod handlerMethod, String fullPath) {
        boolean requiresStore = handlerMethod.hasMethodAnnotation(RequireStore.class)
                || handlerMethod.getBeanType().isAnnotationPresent(RequireStore.class);
        // Endpoints that take the store as a {storeId} path variable don't use the header.
        return requiresStore && !fullPath.contains(STORE_ID_PATH_TEMPLATE);
    }

    /** Class-level base path + method-level path of the handler (e.g. {@code /sellers/stores/{storeId}}). */
    private static String fullPath(HandlerMethod handlerMethod) {
        return firstPath(AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), RequestMapping.class))
                + firstPath(AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), RequestMapping.class));
    }

    private static String firstPath(RequestMapping mapping) {
        return mapping != null && mapping.value().length > 0 ? mapping.value()[0] : "";
    }

    private static void addHeader(Operation operation, String name, String description) {
        boolean present = operation.getParameters() != null && operation.getParameters().stream()
                .anyMatch(p -> HEADER_IN.equals(p.getIn()) && name.equals(p.getName()));
        if (present) {
            return;
        }
        operation.addParametersItem(new Parameter()
                .in(HEADER_IN)
                .name(name)
                .required(true)
                .schema(new StringSchema())
                .description(description));
    }
}
