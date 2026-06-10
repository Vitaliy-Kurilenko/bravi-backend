package ua.com.bravi.bravi.stores.contacts.api;

import ua.com.bravi.bravi.stores.contacts.domain.ContactType;

import java.time.Instant;

public record StoreContactView(
        Long id,
        Long storeId,
        ContactType type,
        String value,
        String comment,
        Instant createdAt,
        Instant updatedAt
) {
}
