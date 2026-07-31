package ua.com.bravi.bravi.shared.media.exception;

import lombok.Getter;

/** A media file fails validation: unsupported content type, empty, too large, or an unexpected key. */
@Getter
public class InvalidMediaUploadException extends RuntimeException {

    private final String field;

    public InvalidMediaUploadException(String field, String message) {
        super(message);
        this.field = field;
    }
}
