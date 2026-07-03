package ua.com.bravi.bravi.identity.domain;

import java.util.UUID;

public record User(
        Long id,
        UUID extId,
        String firstName,
        String lastName,
        String email,
        UserStatus status
) {

    public static User provisionNew(UUID extId,
                                    String firstName,
                                    String lastName,
                                    String email) {
        return new User(null, extId, firstName, lastName, email, UserStatus.ACTIVE);
    }
}
