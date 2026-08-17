package ua.com.bravi.bravi.seller.controller.dto.in;

import jakarta.validation.Valid;

import java.util.List;

/** Full set of values a product carries; anything absent is removed. */
public record ProductAttributesReplaceRequest(
        @Valid List<ProductAttributeValueRequest> attributes
) {
}
