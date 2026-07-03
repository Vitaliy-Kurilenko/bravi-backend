package ua.com.bravi.bravi.seller.exception;

/** Thrown when onboarding completion is attempted before the user's email is verified. */
public class EmailNotVerifiedException extends RuntimeException {

    public EmailNotVerifiedException(String message) {
        super(message);
    }
}
