package ua.com.bravi.bravi.seller.controller.dto.out;

import com.fasterxml.jackson.annotation.JsonProperty;
import ua.com.bravi.bravi.seller.stores.contacts.domain.ContactType;

import java.time.Instant;

public record StoreContactResponse(
        Long id,
        ContactType type,
        String value,
        String comment,
        @JsonProperty("created_at")
        Instant createdAt,
        @JsonProperty("updated_at")
        Instant updatedAt
) {
}
