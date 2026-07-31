package ua.com.bravi.bravi.shared.media.exception;

/** The object is absent from the storage on confirm. */
public class MediaObjectNotFoundException extends RuntimeException {

    public MediaObjectNotFoundException(String message) {
        super(message);
    }
}
