package ua.com.bravi.bravi.seller.controller.dto.in;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ua.com.bravi.bravi.seller.catalog.attributes.domain.AttributeScope;
import ua.com.bravi.bravi.seller.catalog.attributes.domain.AttributeStatus;
import ua.com.bravi.bravi.seller.catalog.attributes.domain.AttributeValueType;

import java.util.List;

public record AttributeCreateRequest(
        @NotBlank String code,
        @NotBlank String name,
        String description,
        @NotNull @JsonProperty("value_type") AttributeValueType valueType,
        AttributeScope scope,
        @JsonProperty("unit_dictionary_code") String unitDictionaryCode,
        @JsonProperty("unit_default_code") String unitDefaultCode,
        @JsonProperty("variant_defining") Boolean variantDefining,
        AttributeStatus status,
        @Valid List<AttributeOptionRequest> options
) {
}
