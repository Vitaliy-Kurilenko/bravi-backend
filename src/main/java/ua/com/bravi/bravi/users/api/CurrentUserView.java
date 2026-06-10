package ua.com.bravi.bravi.users.api;

import java.util.UUID;

public record CurrentUserView(
        Long id,
        UUID extId,
        String type,
        String status,
        String firstName,
        String lastName,
        String email
) {
}
