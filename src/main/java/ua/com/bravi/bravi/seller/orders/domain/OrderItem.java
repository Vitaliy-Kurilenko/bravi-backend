package ua.com.bravi.bravi.seller.orders.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderItem(
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
