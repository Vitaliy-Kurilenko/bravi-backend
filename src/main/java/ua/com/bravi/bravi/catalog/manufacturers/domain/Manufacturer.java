package ua.com.bravi.bravi.catalog.manufacturers.domain;

import java.time.Instant;

public record Manufacturer(
        Long id,
        Long storeId,
        String name,
        String description,
        ManufacturerStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
