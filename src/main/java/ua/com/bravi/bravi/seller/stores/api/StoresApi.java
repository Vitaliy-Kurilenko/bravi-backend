package ua.com.bravi.bravi.seller.stores.api;

import ua.com.bravi.bravi.seller.stores.domain.Store;

import java.util.Optional;

public interface StoresApi {

    /** First store of a seller account (MVP single-store resolution). */
    Optional<Long> findFirstStoreIdByAccountId(Long sellerAccountId);

    Optional<StoreView> getStoreById(Long storeId);

    /** Onboarding: creates a DRAFT store with default settings and returns it. */
    StoreView createDraftStore(Long sellerAccountId, StoreDraft draft);

    /** Onboarding: patches name/description/logo of a DRAFT store. */
    void updateDraftStore(Long storeId, StoreDraft draft);

    void updateStore(Long storeId, Store patch);

    StoreSettings getSettings(Long storeId);

    void updateSettings(Long storeId, StoreSettings patch);

    /** Transitions a store to ACTIVE (onboarding completion). */
    void activateStore(Long storeId);
}
