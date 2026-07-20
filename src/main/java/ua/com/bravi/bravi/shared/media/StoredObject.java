package ua.com.bravi.bravi.shared.media;

/** Метадані вже завантаженого об'єкта (з HEAD). */
public record StoredObject(
        String key,
        String contentType,
        long size
) {
}
