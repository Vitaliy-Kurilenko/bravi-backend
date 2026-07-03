package ua.com.bravi.bravi.seller.orders.exception;

import lombok.Getter;

/** Field-aware 400 для невалідних даних замовлення (невідомий buyer, метод оплати, поле сортування тощо). */
@Getter
public class InvalidOrderRequestException extends RuntimeException {

    private final String field;

    public InvalidOrderRequestException(String field, String message) {
        super(message);
        this.field = field;
    }
}
