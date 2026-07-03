package ua.com.bravi.bravi.identity.api;

import java.util.UUID;

public record CurrentUserView(
        Long id,
        UUID extId,
        String status,
        String firstName,
        String lastName,
        String email
) {
}
