package ua.com.bravi.bravi.seller.controller.dto.out;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AttributeOptionResponse(
        @JsonProperty("public_id")
        String publicId,
        String code,
        String name,
        @JsonProperty("sort_order")
        Integer sortOrder
) {
}
