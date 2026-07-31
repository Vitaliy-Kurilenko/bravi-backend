package ua.com.bravi.bravi.shared.media;

import java.util.List;
import java.util.Optional;

/**
 * Port to the media object storage. A client uploads a file to the storage directly through a
 * presigned link, so the bytes never pass through the application. Implementations stay behind
 * this interface.
 */
public interface MediaStorage {

    /** Generates a storage key under the given prefix and returns a presigned PUT URL for a direct upload. */
    PresignedUpload presignUpload(MediaUploadRequest request);

    /** Returns metadata of the object with the given key, or {@code empty} when no such object exists. */
    Optional<StoredObject> stat(String key);

    /** Returns the keys of all objects under the prefix, or an empty list when there are none. */
    List<String> list(String prefix);

    /** Deletes the object with the given key; idempotent. */
    void delete(String key);

    /** Returns the stable public URL of the object. */
    String publicUrl(String key);
}
