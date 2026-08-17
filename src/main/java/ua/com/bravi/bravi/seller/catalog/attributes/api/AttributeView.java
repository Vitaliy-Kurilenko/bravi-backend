package ua.com.bravi.bravi.seller.catalog.attributes.api;

import ua.com.bravi.bravi.seller.catalog.attributes.domain.AttributeScope;
import ua.com.bravi.bravi.seller.catalog.attributes.domain.AttributeStatus;
import ua.com.bravi.bravi.seller.catalog.attributes.domain.AttributeValueType;

import java.time.Instant;
import java.util.List;

public record AttributeView(
        Long id,
        String publicId,
        Long storeId,
        String templateCode,
        String code,
        String name,
        String description,
        AttributeValueType valueType,
        AttributeScope scope,
        String unitDictionaryCode,
        String unitDefaultCode,
        Boolean variantDefining,
        AttributeStatus status,
        Instant createdAt,
        Instant updatedAt,
        List<AttributeOptionView> options
) {
}
