package ua.com.bravi.bravi.seller.controller.dto.out;

import com.fasterxml.jackson.annotation.JsonProperty;
import ua.com.bravi.bravi.seller.catalog.categories.domain.CategoryStatus;

import java.time.Instant;
import java.util.List;

public record CategoryResponse(
        Long id,
        @JsonProperty("parent_id")
        Long parentId,
        String name,
        String description,
        CategoryStatus status,
        List<CategoryResponse> children,
        @JsonProperty("created_at")
        Instant createdAt,
        @JsonProperty("updated_at")
        Instant updatedAt
) {
}
