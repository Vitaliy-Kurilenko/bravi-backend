package ua.com.bravi.bravi.seller.catalog.attributes.api;

import ua.com.bravi.bravi.seller.catalog.attributes.domain.AttributeValueType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Values a product carries for one attribute, flattened for reading. A value whose definition is no
 * longer offered by the product's category is still returned, with {@code offered} false, so the
 * seller sees it instead of losing it silently.
 */
public record ProductAttributeValueView(
        String attributeId,
        String code,
        String name,
        AttributeValueType valueType,
        Boolean variantDefining,
        boolean offered,
        String valueString,
        BigDecimal valueNumber,
        Boolean valueBoolean,
        LocalDate valueDate,
        String unitCode,
        List<AttributeOptionView> options
) {
}
