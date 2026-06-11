package ua.com.bravi.bravi.catalog.categories.api;

import ua.com.bravi.bravi.catalog.categories.domain.CategoryStatus;

import java.time.Instant;
import java.util.List;

public record CategoryView(
        Long id,
        Long storeId,
        Long parentId,
        String name,
        String description,
        CategoryStatus status,
        Instant createdAt,
        Instant updatedAt,
        List<CategoryView> children
) {
}
