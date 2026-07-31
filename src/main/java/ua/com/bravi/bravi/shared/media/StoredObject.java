package ua.com.bravi.bravi.shared.media;

/** Metadata of an object that is already uploaded to the storage. */
public record StoredObject(
        String key,
        String contentType,
        long size
) {
}
