package ua.com.bravi.bravi.seller.tags.api;

import ua.com.bravi.bravi.seller.tags.domain.TagSortBy;
import ua.com.bravi.bravi.shared.common.SortOrder;

import java.util.List;

public record TagPage(
        List<TagView> data,
        int countPerPage,
        long count,
        int limit,
        int pages,
        int page,
        TagSortBy sortBy,
        SortOrder sortOrder
) {
}
