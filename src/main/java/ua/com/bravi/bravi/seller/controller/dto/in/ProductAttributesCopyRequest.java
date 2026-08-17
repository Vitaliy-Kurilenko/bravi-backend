package ua.com.bravi.bravi.seller.controller.dto.in;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record ProductAttributesCopyRequest(
        @NotBlank @JsonProperty("product_id") String productId
) {
}
