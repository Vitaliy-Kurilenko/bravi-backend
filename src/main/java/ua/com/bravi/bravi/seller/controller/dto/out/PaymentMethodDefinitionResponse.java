package ua.com.bravi.bravi.seller.controller.dto.out;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record PaymentMethodDefinitionResponse(
        String code,
        @JsonProperty("display_name")
        String displayName,
        @JsonProperty("config_schema")
        List<ConfigFieldResponse> configSchema
) {
}
