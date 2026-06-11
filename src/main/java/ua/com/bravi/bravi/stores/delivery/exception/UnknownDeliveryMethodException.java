package ua.com.bravi.bravi.stores.delivery.exception;

import lombok.Getter;

@Getter
public class UnknownDeliveryMethodException extends RuntimeException {

    private final String methodCode;

    public UnknownDeliveryMethodException(String methodCode) {
        super("Unknown delivery method: " + methodCode);
        this.methodCode = methodCode;
    }
}
