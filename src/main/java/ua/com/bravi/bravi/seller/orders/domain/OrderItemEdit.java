package ua.com.bravi.bravi.seller.orders.domain;

import java.math.BigDecimal;

/** Command that adds or edits an order item; on a PATCH any of the fields may be null. */
public record OrderItemEdit(
        Long productId,
        Integer quantity,
        BigDecimal salePrice
) {
}
