package ua.com.bravi.bravi.seller.tags.api;

import ua.com.bravi.bravi.seller.tags.domain.TagStatus;
import ua.com.bravi.bravi.seller.tags.domain.TagTarget;

import java.time.Instant;

/**
 * Read model of one tag. {@code usageCount} is filled on the dictionary path, where it shows how
 * many owners a deletion would untag, and left null when tags are read as part of their owner.
 */
public record TagView(
        Long id,
        String publicId,
        Long storeId,
        TagTarget target,
        String name,
        String color,
        TagStatus status,
        Long usageCount,
        Instant createdAt,
        Instant updatedAt
) {
}
