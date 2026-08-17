package ua.com.bravi.bravi.seller.controller.dto.out;

import com.fasterxml.jackson.annotation.JsonProperty;
import ua.com.bravi.bravi.seller.catalog.attributes.domain.AttributeScope;
import ua.com.bravi.bravi.seller.catalog.attributes.domain.AttributeStatus;
import ua.com.bravi.bravi.seller.catalog.attributes.domain.AttributeValueType;

import java.time.Instant;
import java.util.List;

public record AttributeResponse(
        @JsonProperty("public_id")
        String publicId,
        @JsonProperty("template_code")
        String templateCode,
        String code,
        String name,
        String description,
        @JsonProperty("value_type")
        AttributeValueType valueType,
        AttributeScope scope,
        @JsonProperty("unit_dictionary_code")
        String unitDictionaryCode,
        @JsonProperty("unit_default_code")
        String unitDefaultCode,
        @JsonProperty("variant_defining")
        Boolean variantDefining,
        AttributeStatus status,
        @JsonProperty("created_at")
        Instant createdAt,
        @JsonProperty("updated_at")
        Instant updatedAt,
        List<AttributeOptionResponse> options
) {
}
