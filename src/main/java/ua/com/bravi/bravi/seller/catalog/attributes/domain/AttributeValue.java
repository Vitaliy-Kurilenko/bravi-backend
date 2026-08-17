package ua.com.bravi.bravi.seller.catalog.attributes.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Values a product carries for one attribute, addressed by the attribute's public id. Option-based
 * types fill {@code optionIds}; the remaining types fill exactly one of the literal fields.
 */
public record AttributeValue(
        String attributePublicId,
        String valueString,
        BigDecimal valueNumber,
        Boolean valueBoolean,
        LocalDate valueDate,
        String unitCode,
        List<String> optionIds
) {

    public List<String> optionIds() {
        return optionIds == null ? List.of() : optionIds;
    }
}
