package ua.com.bravi.bravi.seller.orders.api;

import ua.com.bravi.bravi.seller.orders.domain.OrderSortBy;
import ua.com.bravi.bravi.shared.common.SortOrder;

import java.util.List;

public record OrderPage(
        List<OrderView> data,
        int countPerPage,
        long count,
        int limit,
        int pages,
        int page,
        OrderSortBy sortBy,
        SortOrder sortOrder
) {
}
