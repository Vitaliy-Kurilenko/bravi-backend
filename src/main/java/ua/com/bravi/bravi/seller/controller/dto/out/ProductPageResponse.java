package ua.com.bravi.bravi.seller.controller.dto.out;

import com.fasterxml.jackson.annotation.JsonProperty;
import ua.com.bravi.bravi.catalog.products.domain.ProductSortBy;
import ua.com.bravi.bravi.shared.common.SortOrder;

import java.util.List;

public record ProductPageResponse(
        List<ProductResponse> data,
        @JsonProperty("count_per_page")
        int countPerPage,
        long count,
        int limit,
        int pages,
        int page,
        @JsonProperty("sort_by")
        ProductSortBy sortBy,
        @JsonProperty("sort_order")
        SortOrder sortOrder
) {
}
