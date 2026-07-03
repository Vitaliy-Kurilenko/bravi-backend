package ua.com.bravi.bravi.seller.stores.contacts.api;

import ua.com.bravi.bravi.seller.stores.contacts.domain.StoreContact;

import java.util.List;

public interface StoreContactsApi {

    List<StoreContactView> findByStoreId(Long storeId);

    void addContacts(Long storeId, List<StoreContact> contacts);

    /** Replaces all contacts of a store with the given set (onboarding PUT). */
    List<StoreContactView> replaceContacts(Long storeId, List<StoreContact> contacts);

    void updateContact(Long storeId, Long contactId, StoreContact patch);

    void deleteContact(Long storeId, Long contactId);
}
