package ua.com.bravi.bravi.seller.stores.api;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * Request-scoped holder of the store addressed by the request path. Populated by the
 * seller-context interceptor (which resolves {@code {storePublicId}} scoped to the current
 * account) before the handler runs. Null until set → {@code @RequireStore} yields 404.
 */
@Component
@RequestScope
public class CurrentStoreHolder {

    private Long storeId;

    public Long get() {
        return storeId;
    }

    public void set(Long storeId) {
        this.storeId = storeId;
    }

    public void invalidate() {
        this.storeId = null;
    }
}
