package ua.com.bravi.bravi.catalog.products.api;

public record ImageUpload(
        byte[] content,
        String contentType,
        String originalFilename,
        long size,
        boolean primary
) {
}
