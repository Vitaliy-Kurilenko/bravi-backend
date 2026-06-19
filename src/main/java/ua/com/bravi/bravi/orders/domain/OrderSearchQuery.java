package ua.com.bravi.bravi.orders.domain;

import ua.com.bravi.bravi.shared.common.SortOrder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderSearchQuery(
        String search,
        List<Long> buyerIds,
        List<String> paymentMethodCodes,
        List<String> deliveryMethodCodes,
        String recipientName,
        String recipientPhone,
        String recipientEmail,
        List<String> statusCodes,
        BigDecimal minTotal,
        BigDecimal maxTotal,
        Instant createdFrom,
        Instant createdTo,
        OrderSortBy sortBy,
        SortOrder sortOrder,
        int page,
        int limit
) {
}
