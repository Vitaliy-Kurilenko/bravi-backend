package ua.com.bravi.bravi.seller.orders.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * Бізнес-логіка обчислення сум замовлення. Бекенд — джерело правди для {@code subtotal}/{@code total};
 * клієнт передає лише знижку та вартість доставки.
 */
public final class OrderTotals {

    private OrderTotals() {
    }

    /** Сума позицій: Σ salePrice × quantity. */
    public static BigDecimal subtotal(List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return items.stream()
                .map(item -> nvl(item.salePrice()).multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Підсумок: subtotal − discountTotal + shippingTotal (не нижче нуля). */
    public static BigDecimal total(BigDecimal subtotal, BigDecimal discountTotal, BigDecimal shippingTotal) {
        BigDecimal result = nvl(subtotal).subtract(nvl(discountTotal)).add(nvl(shippingTotal));
        return result.signum() < 0 ? BigDecimal.ZERO : result;
    }

    private static BigDecimal nvl(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
