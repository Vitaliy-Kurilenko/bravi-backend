package ua.com.bravi.bravi.shared.media;

/**
 * Request for a presigned upload: the logical category that provides the key prefix and the limits,
 * the owner scope (e.g. {@code "7"} or {@code "7/42"}) and the file metadata.
 */
public record MediaUploadRequest(
        MediaCategory category,
        String scope,
        String contentType,
        long size,
        String originalFilename
) {
}
