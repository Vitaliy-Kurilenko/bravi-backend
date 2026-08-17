package ua.com.bravi.bravi.seller.catalog.attributes.domain;

import ua.com.bravi.bravi.shared.common.SortOrder;

import java.util.List;

public record AttributeSearchQuery(
        String search,
        List<AttributeValueType> valueTypes,
        List<AttributeScope> scopes,
        List<AttributeStatus> statuses,
        AttributeSortBy sortBy,
        SortOrder sortOrder,
        int page,
        int limit
) {
}
