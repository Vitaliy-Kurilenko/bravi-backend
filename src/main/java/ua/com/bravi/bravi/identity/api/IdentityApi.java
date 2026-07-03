package ua.com.bravi.bravi.identity.api;

import java.util.Optional;
import java.util.UUID;

public interface IdentityApi {

    CurrentUserView resolveCurrentUser();

    Optional<CurrentUserView> findByExtId(UUID extId);

    CurrentUserView getById(Long id);
}
