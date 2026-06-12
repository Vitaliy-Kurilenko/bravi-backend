package ua.com.bravi.bravi.catalog.products.api;

import ua.com.bravi.bravi.catalog.products.domain.ProductSortBy;
import ua.com.bravi.bravi.shared.common.SortOrder;

import java.util.List;

public record ProductPage(
        List<ProductView> data,
        int countPerPage,
        long count,
        int limit,
        int pages,
        int page,
        ProductSortBy sortBy,
        SortOrder sortOrder
) {
}
