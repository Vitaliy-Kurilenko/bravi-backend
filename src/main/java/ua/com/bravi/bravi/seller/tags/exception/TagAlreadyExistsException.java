package ua.com.bravi.bravi.seller.tags.exception;

import lombok.Getter;

/** Field-aware 409 for a tag name the store already owns for this target. */
@Getter
public class TagAlreadyExistsException extends RuntimeException {

    private final String field;

    public TagAlreadyExistsException(String field, String message) {
        super(message);
        this.field = field;
    }
}
