package ua.com.bravi.bravi.seller.catalog.products.exception;

import lombok.Getter;

/** Field-aware 409: {@code field} names the product attribute that is already taken. */
@Getter
public class ProductAlreadyExistsException extends RuntimeException {

    private final String field;

    public ProductAlreadyExistsException(String field, String message) {
        super(message);
        this.field = field;
    }
}
