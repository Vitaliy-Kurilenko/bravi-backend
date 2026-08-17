package ua.com.bravi.bravi.seller.controller.dto.out;

import com.fasterxml.jackson.annotation.JsonProperty;
import ua.com.bravi.bravi.seller.catalog.attributes.domain.AttributeSource;

public record CategoryAttributeResponse(
        AttributeResponse attribute,
        AttributeSource source,
        @JsonProperty("source_category_id")
        String sourceCategoryId,
        @JsonProperty("source_category_name")
        String sourceCategoryName,
        @JsonProperty("sort_order")
        Integer sortOrder
) {
}
