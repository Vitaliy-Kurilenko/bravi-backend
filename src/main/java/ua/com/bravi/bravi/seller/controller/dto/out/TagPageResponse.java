package ua.com.bravi.bravi.seller.controller.dto.out;

import com.fasterxml.jackson.annotation.JsonProperty;
import ua.com.bravi.bravi.seller.tags.domain.TagSortBy;
import ua.com.bravi.bravi.shared.common.SortOrder;

import java.util.List;

public record TagPageResponse(
        List<TagResponse> data,
        @JsonProperty("count_per_page")
        int countPerPage,
        long count,
        int limit,
        int pages,
        int page,
        @JsonProperty("sort_by")
        TagSortBy sortBy,
        @JsonProperty("sort_order")
        SortOrder sortOrder
) {
}
