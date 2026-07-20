package ua.com.bravi.bravi.shared.media.exception;

/** На confirm об'єкт відсутній у сховищі (не завантажений або presigned-посилання протухло). */
public class MediaObjectNotFoundException extends RuntimeException {

    public MediaObjectNotFoundException(String message) {
        super(message);
    }
}
