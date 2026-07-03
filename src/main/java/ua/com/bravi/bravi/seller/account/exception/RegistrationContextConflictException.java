package ua.com.bravi.bravi.seller.account.exception;

/** Thrown when a registration request is inconsistent with the existing user/account state. */
public class RegistrationContextConflictException extends RuntimeException {

    public RegistrationContextConflictException(String message) {
        super(message);
    }
}
