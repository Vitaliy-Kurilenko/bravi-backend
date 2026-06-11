package ua.com.bravi.bravi.seller.controller.dto.out;

import com.fasterxml.jackson.annotation.JsonProperty;
import ua.com.bravi.bravi.catalog.manufacturers.domain.ManufacturerStatus;

import java.time.Instant;

public record ManufacturerResponse(
        Long id,
        String name,
        String description,
        ManufacturerStatus status,
        @JsonProperty("created_at")
        Instant createdAt,
        @JsonProperty("updated_at")
        Instant updatedAt
) {
}
