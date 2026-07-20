package ua.com.bravi.bravi.shared.media;

import java.time.Instant;
import java.util.Map;

/** Результат presign: куди PUT-ити, під яким ключем, з якими заголовками і доки посилання дійсне. */
public record PresignedUpload(
        String uploadUrl,
        String storageKey,
        Map<String, String> requiredHeaders,
        Instant expiresAt
) {
}
