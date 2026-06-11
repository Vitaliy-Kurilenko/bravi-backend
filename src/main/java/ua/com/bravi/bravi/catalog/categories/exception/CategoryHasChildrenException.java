package ua.com.bravi.bravi.catalog.categories.exception;

public class CategoryHasChildrenException extends RuntimeException {

    public CategoryHasChildrenException(String message) {
        super(message);
    }
}
