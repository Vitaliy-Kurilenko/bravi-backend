package ua.com.bravi.bravi.seller.component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import ua.com.bravi.bravi.access.api.AccessApi;
import ua.com.bravi.bravi.access.api.AccessContextView;
import ua.com.bravi.bravi.access.api.CurrentAccountHolder;
import ua.com.bravi.bravi.seller.stores.api.CurrentStoreHolder;
import ua.com.bravi.bravi.seller.stores.api.StoreRef;
import ua.com.bravi.bravi.seller.stores.api.StoresApi;
import ua.com.bravi.bravi.shared.exception.NotFoundException;

import java.util.Map;

/**
 * Resolves the request-target store and account from the URL path and populates
 * {@link CurrentStoreHolder} / {@link CurrentAccountHolder} before {@code @PreAuthorize} and
 * {@code @RequireStore} run. Two shapes:
 * <ul>
 *   <li>store-scoped ({@code /stores/{storePublicId}/...}): the account is derived from the store;
 *       an unknown store, or one whose account the user has no ACTIVE membership on, both yield 404
 *       (a store you cannot see is "not found" — no existence leak);</li>
 *   <li>account-scoped ({@code /accounts/{accountPublicId}/seller/...}, onboarding): the account is
 *       resolved directly; no membership → 403.</li>
 * </ul>
 * Replaces the former "first membership / first store" implicit resolution.
 */
@Component
@RequiredArgsConstructor
public class SellerContextInterceptor implements HandlerInterceptor {

    private static final String ACCOUNT_PUBLIC_ID = "accountPublicId";
    private static final String STORE_PUBLIC_ID = "storePublicId";

    private final AccessApi accessApi;
    private final CurrentAccountHolder currentAccountHolder;
    private final StoresApi storesApi;
    private final CurrentStoreHolder currentStoreHolder;

    @Override
    @SuppressWarnings("unchecked")
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        Map<String, String> vars =
                (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (vars == null) {
            return true;
        }

        String storePublicId = vars.get(STORE_PUBLIC_ID);
        if (storePublicId != null) {
            StoreRef store = storesApi.findStoreRefByPublicId(storePublicId)
                    .orElseThrow(() -> new NotFoundException("Store not found"));
            AccessContextView context = accessApi.resolveContext(store.sellerAccountId())
                    .orElseThrow(() -> new NotFoundException("Store not found"));
            currentAccountHolder.set(context);
            currentStoreHolder.set(store.storeId());
            return true;
        }

        String accountPublicId = vars.get(ACCOUNT_PUBLIC_ID);
        if (accountPublicId != null) {
            AccessContextView context = accessApi.resolveContext(accountPublicId)
                    .orElseThrow(() -> new AccessDeniedException("No access to this account"));
            currentAccountHolder.set(context);
        }
        return true;
    }
}
