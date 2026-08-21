package ua.com.bravi.bravi.seller.tags.domain;

import lombok.Builder;
import ua.com.bravi.bravi.shared.common.SortOrder;

import java.util.List;

/** Everything the tag dictionary listing filters, sorts and pages by. */
@Builder
public record TagSearchQuery(
        String search,
        List<TagStatus> statuses,
        TagSortBy sortBy,
        SortOrder sortOrder,
        int page,
        int limit
) {
}
