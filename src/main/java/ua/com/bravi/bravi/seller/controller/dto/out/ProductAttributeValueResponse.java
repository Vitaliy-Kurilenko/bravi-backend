package ua.com.bravi.bravi.seller.controller.dto.out;

import com.fasterxml.jackson.annotation.JsonProperty;
import ua.com.bravi.bravi.seller.catalog.attributes.domain.AttributeValueType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * A value the product carries. {@code offered} is false when the product's category no longer offers
 * the attribute — the value is kept and reported so it can be reviewed rather than lost silently.
 */
public record ProductAttributeValueResponse(
        @JsonProperty("attribute_id")
        String attributeId,
        String code,
        String name,
        @JsonProperty("value_type")
        AttributeValueType valueType,
        @JsonProperty("variant_defining")
        Boolean variantDefining,
        boolean offered,
        @JsonProperty("value_string")
        String valueString,
        @JsonProperty("value_number")
        BigDecimal valueNumber,
        @JsonProperty("value_boolean")
        Boolean valueBoolean,
        @JsonProperty("value_date")
        LocalDate valueDate,
        @JsonProperty("unit_code")
        String unitCode,
        List<AttributeOptionResponse> options
) {
}
