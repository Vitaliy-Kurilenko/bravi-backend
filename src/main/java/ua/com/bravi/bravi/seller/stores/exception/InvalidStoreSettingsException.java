package ua.com.bravi.bravi.seller.stores.exception;

import lombok.Getter;

/** Налаштування магазину містять код, якого немає серед активних елементів довідника. */
@Getter
public class InvalidStoreSettingsException extends RuntimeException {

    private final String field;

    public InvalidStoreSettingsException(String field, String message) {
        super(message);
        this.field = field;
    }
}
