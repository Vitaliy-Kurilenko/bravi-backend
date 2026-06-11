package ua.com.bravi.bravi.stores.payments.exception;

import lombok.Getter;

@Getter
public class UnknownPaymentMethodException extends RuntimeException {

    private final String methodCode;

    public UnknownPaymentMethodException(String methodCode) {
        super("Unknown payment method: " + methodCode);
        this.methodCode = methodCode;
    }
}
