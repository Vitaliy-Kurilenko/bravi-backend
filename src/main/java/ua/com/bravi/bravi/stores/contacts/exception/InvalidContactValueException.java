package ua.com.bravi.bravi.stores.contacts.exception;

import lombok.Getter;

@Getter
public class InvalidContactValueException extends RuntimeException {

    private final String field;

    public InvalidContactValueException(String field, String message) {
        super(message);
        this.field = field;
    }
}
