package ua.com.bravi.bravi.seller.catalog.attributes.domain;

import java.time.Instant;

/** One choice of a SELECT or MULTI_SELECT attribute. Positions are gap-free and zero-based. */
public record AttributeOption(
        Long id,
        String publicId,
        Long attributeId,
        String code,
        String name,
        Integer sortOrder,
        Instant createdAt
) {
}
