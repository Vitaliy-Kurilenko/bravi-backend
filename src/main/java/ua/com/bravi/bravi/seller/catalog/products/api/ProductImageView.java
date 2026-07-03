package ua.com.bravi.bravi.seller.catalog.products.api;

import java.time.Instant;

public record ProductImageView(
        Long id,
        String url,
        String contentType,
        Integer sortOrder,
        Boolean isPrimary,
        Instant createdAt
) {
}
