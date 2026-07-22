package ua.com.bravi.bravi.seller.controller.dto.in;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import ua.com.bravi.bravi.seller.catalog.categories.domain.CategoryStatus;

public record CategoryCreateRequest(
        @NotBlank
        String name,
        String description,
        CategoryStatus status,
        @JsonProperty("parent_public_id")
        String parentPublicId
) {
}
