package ua.com.bravi.bravi.seller.controller.dto.out;

import com.fasterxml.jackson.annotation.JsonProperty;
import ua.com.bravi.bravi.seller.catalog.attributes.domain.AttributeSortBy;
import ua.com.bravi.bravi.shared.common.SortOrder;

import java.util.List;

public record AttributePageResponse(
        List<AttributeResponse> data,
        @JsonProperty("count_per_page")
        int countPerPage,
        long count,
        int limit,
        int pages,
        int page,
        @JsonProperty("sort_by")
        AttributeSortBy sortBy,
        @JsonProperty("sort_order")
        SortOrder sortOrder
) {
}
