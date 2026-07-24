package ua.com.bravi.bravi.seller.catalog.manufacturers.exception;

import lombok.Getter;

/** Field-aware 400 для невалідних параметрів запиту виробників (наприклад невідоме поле сортування). */
@Getter
public class InvalidManufacturerRequestException extends RuntimeException {

    private final String field;

    public InvalidManufacturerRequestException(String field, String message) {
        super(message);
        this.field = field;
    }
}
