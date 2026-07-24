package ua.com.bravi.bravi.seller.catalog.manufacturers.domain;

import ua.com.bravi.bravi.shared.common.SortOrder;

import java.util.List;

public record ManufacturerSearchQuery(
        String search,
        List<ManufacturerStatus> statuses,
        ManufacturerSortBy sortBy,
        SortOrder sortOrder,
        int page,
        int limit
) {
}
