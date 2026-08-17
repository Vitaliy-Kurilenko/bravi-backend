package ua.com.bravi.bravi.seller.catalog.attributes.exception;

import lombok.Getter;

/** Field-aware 400 for invalid attribute data, such as a value that does not match its definition. */
@Getter
public class InvalidAttributeRequestException extends RuntimeException {

    private final String field;

    public InvalidAttributeRequestException(String field, String message) {
        super(message);
        this.field = field;
    }
}
