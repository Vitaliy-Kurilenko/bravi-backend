package ua.com.bravi.bravi.stores.domain;

import ua.com.bravi.bravi.shared.exception.ForbiddenException;

public interface StoreOwned {

    Long getStoreId();

    default void requireOwnedBy(Long currentStoreId) {
        if (!getStoreId().equals(currentStoreId)) {
            throw new ForbiddenException("Resource does not belong to current user's store");
        }
    }
}
