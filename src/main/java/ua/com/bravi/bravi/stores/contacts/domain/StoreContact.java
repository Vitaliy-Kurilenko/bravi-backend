package ua.com.bravi.bravi.stores.contacts.domain;

import java.time.Instant;

public record StoreContact(
        Long id,
        Long storeId,
        ContactType type,
        String value,
        String comment,
        Instant createdAt,
        Instant updatedAt
) {
}
