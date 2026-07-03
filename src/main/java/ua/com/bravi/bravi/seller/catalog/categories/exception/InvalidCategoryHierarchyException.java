package ua.com.bravi.bravi.seller.catalog.categories.exception;

import lombok.Getter;

@Getter
public class InvalidCategoryHierarchyException extends RuntimeException {

    private final String field;

    public InvalidCategoryHierarchyException(String field, String message) {
        super(message);
        this.field = field;
    }
}
