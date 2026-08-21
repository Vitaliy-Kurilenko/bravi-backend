package ua.com.bravi.bravi.seller.controller.dto.in;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** Leaves the product carrying exactly these tags; an empty list clears them. */
public record ProductTagsReplaceRequest(
        @NotNull
        @Valid
        List<ProductTagRequest> tags
) {
}
