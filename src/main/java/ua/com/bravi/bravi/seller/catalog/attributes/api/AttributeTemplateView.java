package ua.com.bravi.bravi.seller.catalog.attributes.api;

import ua.com.bravi.bravi.seller.catalog.attributes.domain.AttributeValueType;

import java.util.List;

/**
 * Library entry a store can adopt. {@code adopted} tells whether the current store already owns a
 * definition made from this template, so the UI can offer "add" or "already added" without a second call.
 */
public record AttributeTemplateView(
        String code,
        String name,
        AttributeValueType valueType,
        String unitDictionaryCode,
        String unitDefaultCode,
        Boolean variantDefining,
        boolean adopted,
        List<AttributeOptionView> options
) {
}
