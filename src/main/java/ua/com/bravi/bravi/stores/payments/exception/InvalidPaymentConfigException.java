package ua.com.bravi.bravi.stores.payments.exception;

import lombok.Getter;

@Getter
public class InvalidPaymentConfigException extends RuntimeException {

    private final String field;

    public InvalidPaymentConfigException(String field, String message) {
        super(message);
        this.field = field;
    }
}
