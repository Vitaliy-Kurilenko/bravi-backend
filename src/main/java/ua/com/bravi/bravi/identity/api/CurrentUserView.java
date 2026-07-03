package ua.com.bravi.bravi.identity.api;

import java.util.UUID;

public record CurrentUserView(
        Long id,
        String publicId,
        UUID extId,
        String status,
        boolean emailVerified,
        String firstName,
        String lastName,
        String email
) {
}
