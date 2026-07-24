package ua.com.bravi.bravi.seller.controller.dto.in;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

/** Body to change a product image's state; only {@code is_primary=true} (promote to primary) is supported. */
public record ProductImageUpdateRequest(
        @NotNull
        @AssertTrue(message = "Only promoting an image to primary is supported")
        @JsonProperty("is_primary")
        Boolean isPrimary
) {
}
