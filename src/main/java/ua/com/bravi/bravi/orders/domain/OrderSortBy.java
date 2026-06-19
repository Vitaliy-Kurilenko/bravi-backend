package ua.com.bravi.bravi.orders.domain;

import lombok.Getter;
import ua.com.bravi.bravi.orders.exception.InvalidOrderRequestException;

/**
 * Whitelist полів сортування замовлень. {@code property} — ім'я властивості {@code OrderEntity}
 * для безпечного {@code Sort} (без прокидання довільного рядка з клієнта).
 */
@Getter
public enum OrderSortBy {
    ID("id"),
    STATUS("statusId"),
    RECIPIENT_NAME("recipientLastName"),
    SHIPPING_METHOD_CODE("deliveryMethodCode"),
    PAYMENT_METHOD_CODE("paymentMethodCode"),
    PARTNER_ID("buyerId"),
    CREATED_AT("createdAt");

    private final String property;

    OrderSortBy(String property) {
        this.property = property;
    }

    /** Парсить зовнішній токен (наприклад {@code created_at}) у enum, case-insensitive. */
    public static OrderSortBy fromParam(String token) {
        for (OrderSortBy value : values()) {
            if (value.name().equalsIgnoreCase(token)) {
                return value;
            }
        }
        throw new InvalidOrderRequestException("sort_by", "Unknown sort field: " + token);
    }
}
