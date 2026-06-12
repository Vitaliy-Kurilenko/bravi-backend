package ua.com.bravi.bravi.catalog.products.domain;

import ua.com.bravi.bravi.shared.common.SortOrder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ProductSearchQuery(
        String search,
        List<Long> categoryIds,
        List<Long> manufacturerIds,
        List<Long> stockStatusIds,
        List<ProductStatus> statuses,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Instant createdFrom,
        Instant createdTo,
        ProductSortBy sortBy,
        SortOrder sortOrder,
        int page,
        int limit
) {
}
