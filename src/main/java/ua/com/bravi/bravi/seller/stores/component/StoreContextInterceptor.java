package ua.com.bravi.bravi.seller.stores.component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import ua.com.bravi.bravi.access.api.AccountContext;
import ua.com.bravi.bravi.seller.stores.api.StoreContext;
import ua.com.bravi.bravi.seller.stores.api.StoreRef;
import ua.com.bravi.bravi.seller.stores.api.StoresApi;
import ua.com.bravi.bravi.shared.common.HttpConstants;
import ua.com.bravi.bravi.shared.exception.NotFoundException;

import java.util.Map;

/**
 * Resolves the request-target store and populates {@link StoreContext}, validating that the store
 * belongs to the account already resolved into {@link AccountContext}. The store public id comes from
 * the {@code {storeId}} path variable when present (store resource: {@code /sellers/stores/{storeId}}),
 * otherwise from the {@code X-Store-Id} header (store-scoped sub-resources: products, categories, …).
 * An unknown store, or one whose account the caller has no ACTIVE membership on, both yield 404 (a
 * store you cannot see is "not found" — no existence leak). Neither present → context stays null, so
 * {@code @RequireStore} yields 404.
 */
@Component
@RequiredArgsConstructor
public class StoreContextInterceptor implements HandlerInterceptor {

    private static final String STORE_ID_PATH_VAR = "storeId";

    private final StoresApi storesApi;
    private final StoreContext storeContext;
    private final AccountContext accountContext;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        String storePublicId = pathVariable(request, STORE_ID_PATH_VAR);
        if (!StringUtils.hasText(storePublicId)) {
            storePublicId = request.getHeader(HttpConstants.STORE_ID_HEADER);
        }
        if (!StringUtils.hasText(storePublicId)) {
            return true;
        }
        StoreRef store = storesApi.findStoreRefByPublicId(storePublicId)
                .orElseThrow(() -> new NotFoundException("Store not found"));
        Long accountId = accountContext.getAccountId();
        if (accountId == null || !store.sellerAccountId().equals(accountId)) {
            throw new NotFoundException("Store not found");
        }
        storeContext.set(store.storeId());
        return true;
    }

    @SuppressWarnings("unchecked")
    private static String pathVariable(HttpServletRequest request, String name) {
        Object vars = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        return vars instanceof Map<?, ?> map ? ((Map<String, String>) map).get(name) : null;
    }
}
