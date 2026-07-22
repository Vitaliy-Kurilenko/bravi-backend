package ua.com.bravi.bravi.seller.stores.api;

import ua.com.bravi.bravi.seller.stores.domain.Store;
import ua.com.bravi.bravi.shared.media.PresignedUpload;

import java.util.List;
import java.util.Optional;

public interface StoresApi {

    /** First store of a seller account (used by onboarding, which owns a single store). */
    Optional<Long> findFirstStoreIdByAccountId(Long sellerAccountId);

    /** All stores of a seller account (internal accounts.id), ordered by id. */
    List<StoreView> getStoresByAccountId(Long sellerAccountId);

    /** Store id + owning account for a store public id; the caller then validates the user's membership. */
    Optional<StoreRef> findStoreRefByPublicId(String storePublicId);

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

    /** Validates the declared logo and returns a presigned PUT URL for direct client upload. */
    PresignedUpload presignLogoUpload(Long storeId, LogoUpload upload);

    /** Confirms an uploaded logo: re-validates the stored object, attaches it, drops the previous one. */
    StoreView confirmLogo(Long storeId, String storageKey);

    /** Removes the store logo (deletes the object and clears the columns). */
    StoreView removeLogo(Long storeId);
}
