package ua.com.bravi.bravi.seller.orders.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * Business logic that computes order amounts. The backend is the source of truth for
 * {@code subtotal} and {@code total}, while the client supplies only the discount and the shipping cost.
 */
public final class OrderTotals {

    private OrderTotals() {
    }

    /** Sum over the items of sale price multiplied by quantity. */
    public static BigDecimal subtotal(List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return items.stream()
                .map(item -> nvl(item.salePrice()).multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Total: subtotal minus discount plus shipping, never below zero. */
    public static BigDecimal total(BigDecimal subtotal, BigDecimal discountTotal, BigDecimal shippingTotal) {
        BigDecimal result = nvl(subtotal).subtract(nvl(discountTotal)).add(nvl(shippingTotal));
        return result.signum() < 0 ? BigDecimal.ZERO : result;
    }

    private static BigDecimal nvl(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
