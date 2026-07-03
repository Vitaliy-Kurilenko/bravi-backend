package ua.com.bravi.bravi.seller.catalog.manufacturers.api;

import ua.com.bravi.bravi.seller.catalog.manufacturers.domain.ManufacturerStatus;

import java.time.Instant;

public record ManufacturerView(
        Long id,
        Long storeId,
        String name,
        String description,
        ManufacturerStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
