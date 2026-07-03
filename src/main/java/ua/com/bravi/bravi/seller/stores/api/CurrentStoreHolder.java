package ua.com.bravi.bravi.seller.stores.api;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;
import ua.com.bravi.bravi.shared.component.InvocationContext;

@Component
@RequestScope
@RequiredArgsConstructor
public class CurrentStoreHolder {

    private final InvocationContext invocationContext;
    @Lazy
    private final StoresApi storesApi;

    private Long storeId;
    private boolean resolved;

    public Long get() {
        if (resolved) {
            return storeId;
        }
        Long userId = invocationContext.getUserId();
        if (userId == null) {
            return null; // user ще не resolved — не кешуємо, даємо повторити пізніше
        }
        storeId = storesApi.findStoreIdByUserId(userId).orElse(null);
        resolved = true;
        return storeId;
    }

    public void set(Long storeId) {
        this.storeId = storeId;
        this.resolved = true;
    }

    public void invalidate() {
        this.storeId = null;
        this.resolved = false;
    }
}
