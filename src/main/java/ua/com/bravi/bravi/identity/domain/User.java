package ua.com.bravi.bravi.identity.domain;

import java.util.UUID;

public record User(
        Long id,
        String publicId,
        UUID extId,
        String firstName,
        String lastName,
        String email,
        boolean emailVerified,
        UserStatus status
) {

    /** New user provisioned from an external identity (Keycloak); email not yet verified. */
    public static User register(String publicId,
                                UUID extId,
                                String firstName,
                                String lastName,
                                String email) {
        return new User(null, publicId, extId, firstName, lastName, email, false, UserStatus.ACTIVE);
    }
}
