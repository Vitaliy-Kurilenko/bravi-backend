package ua.com.bravi.bravi.seller.controller.dto.out;

import com.fasterxml.jackson.annotation.JsonProperty;
import ua.com.bravi.bravi.seller.catalog.attributes.domain.AttributeValueType;

import java.util.List;

public record AttributeTemplateResponse(
        String code,
        String name,
        @JsonProperty("value_type")
        AttributeValueType valueType,
        @JsonProperty("unit_dictionary_code")
        String unitDictionaryCode,
        @JsonProperty("unit_default_code")
        String unitDefaultCode,
        @JsonProperty("variant_defining")
        Boolean variantDefining,
        boolean adopted,
        List<AttributeOptionResponse> options
) {
}
