package ua.com.bravi.bravi.stores.contacts.api;

import ua.com.bravi.bravi.stores.contacts.domain.StoreContact;

import java.util.List;

public interface StoreContactsApi {

    List<StoreContactView> findByStoreId(Long storeId);

    void addContacts(Long storeId, List<StoreContact> contacts);

    void updateContact(Long storeId, Long contactId, StoreContact patch);

    void deleteContact(Long storeId, Long contactId);
}
