package ua.com.bravi.bravi.stores.api;

import ua.com.bravi.bravi.shared.exception.ForbiddenException;
import ua.com.bravi.bravi.stores.domain.Store;

import java.util.Optional;

public interface StoresApi {

    Optional<Long> findStoreIdByUserId(Long userId);

    Optional<StoreView> findStoreByUserId(Long userId);

    Optional<StoreView> getStoreById(Long storeId);

    Long createStore(Long sellerId, Store store);

    void updateStore(Long storeId, Store patch);

    /**
     * Throws {@link ForbiddenException} if the store does not belong to the given user.
     */
    void requireOwnership(Long storeId, Long userId);
}
