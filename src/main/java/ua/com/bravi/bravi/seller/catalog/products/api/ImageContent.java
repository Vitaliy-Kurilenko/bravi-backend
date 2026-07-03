package ua.com.bravi.bravi.seller.catalog.products.api;

public record ImageContent(
        byte[] content,
        String contentType,
        String filename
) {
}
