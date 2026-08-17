package ua.com.bravi.bravi.seller.controller.dto.in;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Attributes to offer in a category. Template codes name entries of the shared library; any the store
 * does not own yet are copied into it as part of the same call.
 */
public record CategoryAttributeBindRequest(
        @JsonProperty("attribute_ids") List<String> attributeIds,
        @JsonProperty("template_codes") List<String> templateCodes
) {
}
