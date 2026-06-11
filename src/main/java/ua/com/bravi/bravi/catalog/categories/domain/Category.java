package ua.com.bravi.bravi.catalog.categories.domain;

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
