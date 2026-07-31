package ua.com.bravi.bravi.seller.stores.exception;

import lombok.Getter;

/** Store settings carry a code that is not among the active items of the dictionary. */
@Getter
public class InvalidStoreSettingsException extends RuntimeException {

    private final String field;

    public InvalidStoreSettingsException(String field, String message) {
        super(message);
        this.field = field;
    }
}
