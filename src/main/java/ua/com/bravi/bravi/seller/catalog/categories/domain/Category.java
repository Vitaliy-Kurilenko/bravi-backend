package ua.com.bravi.bravi.seller.catalog.categories.domain;

import java.time.Instant;

public record Category(
        Long id,
        String publicId,
        Long storeId,
        Long parentId,
        String parentPublicId,
        String name,
        String description,
        CategoryStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
