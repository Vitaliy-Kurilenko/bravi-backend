package ua.com.bravi.bravi.seller.controller.dto.in;

import jakarta.validation.constraints.NotBlank;

public record AttributeOptionRequest(
        @NotBlank String code,
        @NotBlank String name
) {
}
