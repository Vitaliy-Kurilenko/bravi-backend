package ua.com.bravi.bravi.orders.api;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderItemView(
        Long id,
        Long productId,
        String sku,
        String code,
        String name,
        Integer quantity,
        BigDecimal partnerPrice,
        BigDecimal salePrice,
        Instant createdAt,
        Instant updatedAt
) {
}
