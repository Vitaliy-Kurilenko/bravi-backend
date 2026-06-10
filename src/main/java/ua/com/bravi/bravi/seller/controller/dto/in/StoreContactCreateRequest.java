package ua.com.bravi.bravi.seller.controller.dto.in;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ua.com.bravi.bravi.stores.contacts.domain.ContactType;

public record StoreContactCreateRequest(
        @NotNull
        ContactType type,
        @NotBlank
        String value,
        String comment
) {
}
