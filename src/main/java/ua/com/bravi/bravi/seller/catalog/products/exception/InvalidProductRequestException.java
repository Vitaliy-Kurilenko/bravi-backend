package ua.com.bravi.bravi.seller.catalog.products.exception;

import lombok.Getter;

/** Field-aware 400 for invalid product data, such as an image upload or a search parameter. */
@Getter
public class InvalidProductRequestException extends RuntimeException {

    private final String field;

    public InvalidProductRequestException(String field, String message) {
        super(message);
        this.field = field;
    }
}
