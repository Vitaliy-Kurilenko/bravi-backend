package ua.com.bravi.bravi.seller.controller.dto.in;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/** Body to attach a previously uploaded image to the end of a product's gallery. */
public record ProductImageAttachRequest(
        @NotBlank
        @JsonProperty("storage_key")
        String storageKey
) {
}
