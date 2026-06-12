package ua.com.bravi.bravi.seller.controller.dto.out;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record ProductImageResponse(
        Long id,
        String url,
        @JsonProperty("content_type")
        String contentType,
        @JsonProperty("sort_order")
        Integer sortOrder,
        @JsonProperty("is_primary")
        Boolean isPrimary,
        @JsonProperty("created_at")
        Instant createdAt
) {
}
