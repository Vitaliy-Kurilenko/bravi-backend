package ua.com.bravi.bravi.users.api;

import java.util.Optional;
import java.util.UUID;

public interface UsersApi {

    CurrentUserView resolveCurrentUser();

    Optional<CurrentUserView> findByExtId(UUID extId);

    CurrentUserView getById(Long id);
}
