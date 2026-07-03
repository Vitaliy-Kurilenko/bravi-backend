package ua.com.bravi.bravi.seller.orders.domain;

import java.math.BigDecimal;

/** Команда додавання/редагування позиції замовлення (productId/quantity/salePrice; для PATCH — будь-яке поле nullable). */
public record OrderItemEdit(
        Long productId,
        Integer quantity,
        BigDecimal salePrice
) {
}
