package ua.com.bravi.bravi.seller.stores.delivery.exception;

import lombok.Getter;

@Getter
public class InvalidDeliveryConfigException extends RuntimeException {

    private final String field;

    public InvalidDeliveryConfigException(String field, String message) {
        super(message);
        this.field = field;
    }
}
