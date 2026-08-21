package ua.com.bravi.bravi.seller.catalog.products.domain;

import ua.com.bravi.bravi.seller.tags.domain.TagsMatch;
import ua.com.bravi.bravi.shared.common.SortOrder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ProductSearchQuery(
        String search,
        List<String> categoryIds,
        List<String> manufacturerIds,
        List<Long> stockStatusIds,
        List<ProductStatus> statuses,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Instant createdFrom,
        Instant createdTo,
        /** Three-state: null leaves the filter off, true keeps only discounted products, false excludes them. */
        Boolean hasActiveDiscount,
        List<String> tagIds,
        /** How tagIds combine; null reads as ANY. */
        TagsMatch tagsMatch,
        ProductSortBy sortBy,
        SortOrder sortOrder,
        int page,
        int limit
) {
}
