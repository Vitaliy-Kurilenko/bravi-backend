package ua.com.bravi.bravi.seller.catalog.manufacturers.api;

import ua.com.bravi.bravi.seller.catalog.manufacturers.domain.ManufacturerSortBy;
import ua.com.bravi.bravi.shared.common.SortOrder;

import java.util.List;

public record ManufacturerPage(
        List<ManufacturerView> data,
        int countPerPage,
        long count,
        int limit,
        int pages,
        int page,
        ManufacturerSortBy sortBy,
        SortOrder sortOrder
) {
}
