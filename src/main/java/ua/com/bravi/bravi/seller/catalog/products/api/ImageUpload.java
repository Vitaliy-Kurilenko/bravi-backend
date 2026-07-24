package ua.com.bravi.bravi.seller.catalog.products.api;

/** Declared image metadata for presign (validated before the upload and again on confirm). */
public record ImageUpload(
        String contentType,
        long size,
        String originalFilename
) {
}
