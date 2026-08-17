package ua.com.bravi.bravi.seller.controller.dto.in;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Values submitted for one attribute. Option-based attributes fill {@code option_ids}; the remaining
 * types fill the field matching their value type.
 */
public record ProductAttributeValueRequest(
        @NotBlank @JsonProperty("attribute_id") String attributeId,
        @JsonProperty("value_string") String valueString,
        @JsonProperty("value_number") BigDecimal valueNumber,
        @JsonProperty("value_boolean") Boolean valueBoolean,
        @JsonProperty("value_date") LocalDate valueDate,
        @JsonProperty("unit_code") String unitCode,
        @JsonProperty("option_ids") List<String> optionIds
) {
}
