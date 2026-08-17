package ua.com.bravi.bravi.seller.catalog.attributes.exception;

import lombok.Getter;

/** Raised when an attribute or option still carries product values and therefore cannot be removed. */
@Getter
public class AttributeInUseException extends RuntimeException {

    private final String field;

    public AttributeInUseException(String field, String message) {
        super(message);
        this.field = field;
    }
}
