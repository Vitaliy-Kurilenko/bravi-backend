package ua.com.bravi.bravi.seller.tags.domain;

import lombok.Builder;

import java.time.Instant;

/** A label a store owns. Everything but the name, the colour and the status is bookkeeping. */
@Builder
public record Tag(
        Long id,
        String publicId,
        Long storeId,
        TagTarget target,
        String name,
        String color,
        TagStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
