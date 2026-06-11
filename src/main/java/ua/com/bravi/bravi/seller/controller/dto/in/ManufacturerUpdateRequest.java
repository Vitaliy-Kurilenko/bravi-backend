package ua.com.bravi.bravi.seller.controller.dto.in;

import ua.com.bravi.bravi.catalog.manufacturers.domain.ManufacturerStatus;

public record ManufacturerUpdateRequest(
        String name,
        String description,
        ManufacturerStatus status
) {
}
