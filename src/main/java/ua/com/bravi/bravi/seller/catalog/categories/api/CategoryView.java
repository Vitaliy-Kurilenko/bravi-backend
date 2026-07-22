package ua.com.bravi.bravi.seller.catalog.categories.api;

import ua.com.bravi.bravi.seller.catalog.categories.domain.CategoryStatus;

import java.time.Instant;
import java.util.List;

public record CategoryView(
        Long id,
        String publicId,
        Long storeId,
        Long parentId,
        String parentPublicId,
        String name,
        String description,
        CategoryStatus status,
        Instant createdAt,
        Instant updatedAt,
        List<CategoryView> children
) {
}
