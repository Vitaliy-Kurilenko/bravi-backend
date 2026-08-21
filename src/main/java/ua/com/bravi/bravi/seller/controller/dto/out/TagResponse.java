package ua.com.bravi.bravi.seller.controller.dto.out;

import com.fasterxml.jackson.annotation.JsonProperty;
import ua.com.bravi.bravi.seller.tags.domain.TagStatus;

import java.time.Instant;

/** {@code usage_count} shows how many things a deletion would untag; it is null outside the dictionary. */
public record TagResponse(
        @JsonProperty("public_id")
        String publicId,
        String name,
        String color,
        TagStatus status,
        @JsonProperty("usage_count")
        Long usageCount,
        @JsonProperty("created_at")
        Instant createdAt,
        @JsonProperty("updated_at")
        Instant updatedAt
) {
}
