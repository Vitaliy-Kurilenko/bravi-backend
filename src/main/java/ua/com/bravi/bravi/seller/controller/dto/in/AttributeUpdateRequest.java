package ua.com.bravi.bravi.seller.controller.dto.in;

import com.fasterxml.jackson.annotation.JsonProperty;
import ua.com.bravi.bravi.seller.catalog.attributes.domain.AttributeScope;
import ua.com.bravi.bravi.seller.catalog.attributes.domain.AttributeStatus;

/** Code and value type are fixed after creation, so they are absent here. */
public record AttributeUpdateRequest(
        String name,
        String description,
        AttributeScope scope,
        @JsonProperty("unit_dictionary_code") String unitDictionaryCode,
        @JsonProperty("unit_default_code") String unitDefaultCode,
        @JsonProperty("variant_defining") Boolean variantDefining,
        AttributeStatus status
) {
}
