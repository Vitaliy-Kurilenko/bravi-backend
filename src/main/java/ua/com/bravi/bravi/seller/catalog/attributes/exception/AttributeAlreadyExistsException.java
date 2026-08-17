package ua.com.bravi.bravi.seller.catalog.attributes.exception;

import lombok.Getter;

/** Raised when an attribute code, or an option code within an attribute, is already taken. */
@Getter
public class AttributeAlreadyExistsException extends RuntimeException {

    private final String field;

    public AttributeAlreadyExistsException(String field, String message) {
        super(message);
        this.field = field;
    }
}
