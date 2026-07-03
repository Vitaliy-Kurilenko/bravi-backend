package ua.com.bravi.bravi.seller.catalog.categories.domain;

import java.time.Instant;

public record Category(
        Long id,
        Long storeId,
        Long parentId,
        String name,
        String description,
        CategoryStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
