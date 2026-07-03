package ua.com.bravi.bravi.seller.catalog.products.api;

public record ImageUpload(
        byte[] content,
        String contentType,
        String originalFilename,
        long size,
        boolean primary
) {
}
