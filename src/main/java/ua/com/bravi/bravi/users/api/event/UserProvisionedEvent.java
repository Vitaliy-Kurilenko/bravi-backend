package ua.com.bravi.bravi.users.api.event;

import java.time.Instant;
import java.util.UUID;

public record UserProvisionedEvent(
        Long userId,
        UUID extId,
        String type,
        Instant at
) {
}
