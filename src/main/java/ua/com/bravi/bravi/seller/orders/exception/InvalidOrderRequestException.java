package ua.com.bravi.bravi.seller.orders.exception;

import lombok.Getter;

/** Field-aware 400 for invalid order data, such as an unknown buyer, payment method or sort field. */
@Getter
public class InvalidOrderRequestException extends RuntimeException {

    private final String field;

    public InvalidOrderRequestException(String field, String message) {
        super(message);
        this.field = field;
    }
}
