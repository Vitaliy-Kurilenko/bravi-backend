package ua.com.bravi.bravi.seller.stores.api;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;
import ua.com.bravi.bravi.access.api.CurrentAccountHolder;

@Component
@RequestScope
public class CurrentStoreHolder {

    private final CurrentAccountHolder currentAccountHolder;
    private final StoresApi storesApi;

    private Long storeId;
    private boolean resolved;

    public CurrentStoreHolder(CurrentAccountHolder currentAccountHolder, @Lazy StoresApi storesApi) {
        this.currentAccountHolder = currentAccountHolder;
        this.storesApi = storesApi;
    }

    public Long get() {
        if (resolved) {
            return storeId;
        }
        Long sellerAccountId = currentAccountHolder.getAccountId();
        if (sellerAccountId == null) {
            return null; // account ще не resolved / користувач не онбордився — не кешуємо
        }
        storeId = storesApi.findFirstStoreIdByAccountId(sellerAccountId).orElse(null);
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
