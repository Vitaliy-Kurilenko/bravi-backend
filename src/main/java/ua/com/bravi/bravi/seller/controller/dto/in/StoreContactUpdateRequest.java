package ua.com.bravi.bravi.seller.controller.dto.in;

import ua.com.bravi.bravi.seller.stores.contacts.domain.ContactType;

public record StoreContactUpdateRequest(
        ContactType type,
        String value,
        String comment
) {
}
