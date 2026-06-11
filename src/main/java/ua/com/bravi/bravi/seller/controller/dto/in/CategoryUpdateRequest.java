package ua.com.bravi.bravi.seller.controller.dto.in;

import com.fasterxml.jackson.annotation.JsonProperty;
import ua.com.bravi.bravi.catalog.categories.domain.CategoryStatus;

public record CategoryUpdateRequest(
        String name,
        String description,
        CategoryStatus status,
        @JsonProperty("parent_id")
        Long parentId
) {
}
