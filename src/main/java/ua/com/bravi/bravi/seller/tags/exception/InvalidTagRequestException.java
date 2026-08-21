package ua.com.bravi.bravi.seller.tags.exception;

import lombok.Getter;

/** Field-aware 400 for tag data that breaks a business rule. */
@Getter
public class InvalidTagRequestException extends RuntimeException {

    private final String field;

    public InvalidTagRequestException(String field, String message) {
        super(message);
        this.field = field;
    }
}
