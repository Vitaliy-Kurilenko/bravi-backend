package ua.com.bravi.bravi.identity.api.event;

import java.time.Instant;
import java.util.UUID;

public record UserProvisionedEvent(
        Long userId,
        UUID extId,
        Instant at
) {
}
