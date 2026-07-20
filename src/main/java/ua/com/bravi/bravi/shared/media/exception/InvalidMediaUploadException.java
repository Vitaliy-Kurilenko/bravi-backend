package ua.com.bravi.bravi.shared.media.exception;

import lombok.Getter;

/** Медіа-файл не проходить валідацію (недозволений тип, порожній чи завеликий) або ключ не той. */
@Getter
public class InvalidMediaUploadException extends RuntimeException {

    private final String field;

    public InvalidMediaUploadException(String field, String message) {
        super(message);
        this.field = field;
    }
}
