package ua.com.bravi.bravi.seller.catalog.attributes.api;

import ua.com.bravi.bravi.seller.catalog.attributes.domain.AttributeSortBy;
import ua.com.bravi.bravi.shared.common.SortOrder;

import java.util.List;

public record AttributePage(
        List<AttributeView> data,
        int countPerPage,
        long count,
        int limit,
        int pages,
        int page,
        AttributeSortBy sortBy,
        SortOrder sortOrder
) {
}
