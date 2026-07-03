package ua.com.bravi.bravi.seller.controller.dto.in;

import jakarta.validation.constraints.NotBlank;
import ua.com.bravi.bravi.seller.catalog.manufacturers.domain.ManufacturerStatus;

public record ManufacturerCreateRequest(
        @NotBlank
        String name,
        String description,
        ManufacturerStatus status
) {
}
