package ua.com.bravi.bravi.domain.user;

import java.util.UUID;

public record User(
        Long id,
        UUID extId,
        UserType type,
        String firstName,
        String lastName,
        String email,
        UserStatus status
) {

    public static User provisionNew(UUID extId,
                                    String firstName,
                                    String lastName,
                                    String email,
                                    UserType type) {
        return new User(null, extId, type, firstName, lastName, email, UserStatus.ACTIVE);
    }
}
