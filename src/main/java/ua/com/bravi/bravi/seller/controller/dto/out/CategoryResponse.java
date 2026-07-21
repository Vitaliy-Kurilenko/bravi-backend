package ua.com.bravi.bravi.seller.controller.dto.out;

import com.fasterxml.jackson.annotation.JsonProperty;
import ua.com.bravi.bravi.seller.catalog.categories.domain.CategoryStatus;

import java.time.Instant;
import java.util.List;

public record CategoryResponse(
        @JsonProperty("public_id")
        String publicId,
        @JsonProperty("parent_public_id")
        String parentPublicId,
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
