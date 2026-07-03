package ua.com.bravi.bravi.seller.exception;

/** Thrown when onboarding tries to create a store for a seller account that already has one. */
public class StoreAlreadyExistsException extends RuntimeException {

    public StoreAlreadyExistsException(String message) {
        super(message);
    }
}
