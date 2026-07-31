package ua.com.bravi.bravi.seller.stores.api;

/** Logo metadata declared by the client for a presign; validated both before and after the upload. */
public record LogoUpload(
        String contentType,
        long size,
        String originalFilename
) {
}
