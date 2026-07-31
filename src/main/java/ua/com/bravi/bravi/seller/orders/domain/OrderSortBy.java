package ua.com.bravi.bravi.seller.orders.domain;

import lombok.Getter;
import ua.com.bravi.bravi.seller.orders.exception.InvalidOrderRequestException;

/**
 * Whitelist of order sort fields. {@code property} is the {@code OrderEntity} property name used
 * to build a {@code Sort}, so an arbitrary client string never reaches the query.
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

    /** Parses an external token such as {@code created_at} into the enum, case-insensitively. */
    public static OrderSortBy fromParam(String token) {
        for (OrderSortBy value : values()) {
            if (value.name().equalsIgnoreCase(token)) {
                return value;
            }
        }
        throw new InvalidOrderRequestException("sort_by", "Unknown sort field: " + token);
    }
}
