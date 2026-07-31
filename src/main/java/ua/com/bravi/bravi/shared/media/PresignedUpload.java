package ua.com.bravi.bravi.shared.media;

import java.time.Instant;
import java.util.Map;

/** Result of a presign: where to PUT, under which key, with which headers and until when the link is valid. */
public record PresignedUpload(
        String uploadUrl,
        String storageKey,
        Map<String, String> requiredHeaders,
        Instant expiresAt
) {
}
