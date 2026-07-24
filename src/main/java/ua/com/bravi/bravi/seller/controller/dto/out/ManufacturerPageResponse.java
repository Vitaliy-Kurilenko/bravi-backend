package ua.com.bravi.bravi.seller.controller.dto.out;

import com.fasterxml.jackson.annotation.JsonProperty;
import ua.com.bravi.bravi.seller.catalog.manufacturers.domain.ManufacturerSortBy;
import ua.com.bravi.bravi.shared.common.SortOrder;

import java.util.List;

public record ManufacturerPageResponse(
        List<ManufacturerResponse> data,
        @JsonProperty("count_per_page")
        int countPerPage,
        long count,
        int limit,
        int pages,
        int page,
        @JsonProperty("sort_by")
        ManufacturerSortBy sortBy,
        @JsonProperty("sort_order")
        SortOrder sortOrder
) {
}
