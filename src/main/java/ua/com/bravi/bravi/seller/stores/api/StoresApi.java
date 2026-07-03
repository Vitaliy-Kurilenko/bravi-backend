package ua.com.bravi.bravi.seller.stores.api;

import ua.com.bravi.bravi.seller.stores.domain.Store;

import java.util.Optional;

public interface StoresApi {

    /** First store of a seller account (MVP single-store resolution). */
    Optional<Long> findFirstStoreIdByAccountId(Long sellerAccountId);

    Optional<StoreView> getStoreById(Long storeId);

    Long createStore(Long sellerAccountId, Store store);

    void updateStore(Long storeId, Store patch);
}
