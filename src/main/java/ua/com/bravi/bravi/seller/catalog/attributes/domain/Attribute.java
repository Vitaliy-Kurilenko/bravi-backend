package ua.com.bravi.bravi.seller.catalog.attributes.domain;

import java.time.Instant;

/**
 * Definition of a characteristic a product can carry. {@code code} is unique within the store and
 * fixed after creation, so it can serve as the mapping key towards external marketplaces;
 * {@code templateCode} is kept when the definition was adopted from the shared library.
 */
public record Attribute(
        Long id,
        String publicId,
        Long storeId,
        String templateCode,
        String code,
        String name,
        String description,
        AttributeValueType valueType,
        AttributeScope scope,
        String unitDictionaryCode,
        String unitDefaultCode,
        Boolean variantDefining,
        AttributeStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
