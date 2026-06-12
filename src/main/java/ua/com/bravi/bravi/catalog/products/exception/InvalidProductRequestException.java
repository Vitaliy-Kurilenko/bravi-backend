package ua.com.bravi.bravi.catalog.products.exception;

import lombok.Getter;

/** Field-aware 400 для невалідних даних товару (завантаження фото, параметри пошуку тощо). */
@Getter
public class InvalidProductRequestException extends RuntimeException {

    private final String field;

    public InvalidProductRequestException(String field, String message) {
        super(message);
        this.field = field;
    }
}
