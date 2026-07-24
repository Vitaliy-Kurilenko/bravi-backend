package ua.com.bravi.bravi.seller.controller.dto.out;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Map;

/** Presigned upload instructions: where to PUT the image, under which key, with which headers. */
public record ProductImageUploadUrlResponse(
        @JsonProperty("upload_url")
        String uploadUrl,
        @JsonProperty("storage_key")
        String storageKey,
        @JsonProperty("expires_at")
        Instant expiresAt,
        Map<String, String> headers
) {
}
