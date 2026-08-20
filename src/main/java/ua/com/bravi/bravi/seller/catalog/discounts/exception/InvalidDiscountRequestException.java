package ua.com.bravi.bravi.seller.catalog.discounts.exception;

import lombok.Getter;

/** Field-aware 400 for discount data that breaks a business rule. */
@Getter
public class InvalidDiscountRequestException extends RuntimeException {

    private final String field;

    public InvalidDiscountRequestException(String field, String message) {
        super(message);
        this.field = field;
    }
}
