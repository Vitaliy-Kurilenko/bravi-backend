package ua.com.bravi.bravi.shared.media;

import org.springframework.util.unit.DataSize;
import ua.com.bravi.bravi.shared.media.exception.InvalidMediaUploadException;

import java.util.Set;

/**
 * Registry of logical media types: where an object lives in the storage (the key prefix) and which
 * limits apply to the file. A single bucket is split by prefix, and every entity that uploads media
 * passes its own category. New types are declared here, while authorization and attaching stay in
 * the owning module.
 */
public enum MediaCategory {

    STORE_LOGO("store-logos", Set.of("image/png", "image/jpeg", "image/webp"), DataSize.ofMegabytes(5)),
    PRODUCT_IMAGE("product-images", Set.of("image/png", "image/jpeg", "image/webp"), DataSize.ofMegabytes(5));

    private static final String FIELD = "file";

    private final String prefix;
    private final Set<String> allowedContentTypes;
    private final long maxSizeBytes;

    MediaCategory(String prefix, Set<String> allowedContentTypes, DataSize maxSize) {
        this.prefix = prefix;
        this.allowedContentTypes = allowedContentTypes;
        this.maxSizeBytes = maxSize.toBytes();
    }

    /** Key prefix for a particular owner, e.g. {@code STORE_LOGO.keyPrefix("7") → "store-logos/7"}. */
    public String keyPrefix(String scope) {
        return prefix + "/" + scope;
    }

    /** Validates content type and size: on presign against the declared values, on confirm against the stored ones. */
    public void validate(String contentType, long size) {
        if (contentType == null || !allowedContentTypes.contains(contentType.toLowerCase())) {
            throw new InvalidMediaUploadException(FIELD, "Unsupported media type: " + contentType);
        }
        if (size <= 0) {
            throw new InvalidMediaUploadException(FIELD, "Empty file");
        }
        if (size > maxSizeBytes) {
            throw new InvalidMediaUploadException(FIELD, "File exceeds the maximum allowed size");
        }
    }
}
