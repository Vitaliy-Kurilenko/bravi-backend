package ua.com.bravi.bravi.catalog.products.api;

public record ImageContent(
        byte[] content,
        String contentType,
        String filename
) {
}
