package ua.com.bravi.bravi.identity.api;

import java.util.Optional;
import java.util.UUID;

public interface IdentityApi {

    /** Lookup-only resolution of the current request's user; {@code null} if not registered. */
    CurrentUserView resolveCurrentUser();

    /**
     * Provisions a user from an external identity (Keycloak). Idempotent by {@code keycloakUserId}:
     * returns the existing user if already present. Created with {@code emailVerified=false}, status ACTIVE.
     */
    CurrentUserView provisionUser(UUID keycloakUserId, String email, String firstName, String lastName);

    Optional<CurrentUserView> findByExtId(UUID extId);

    Optional<CurrentUserView> findByEmail(String email);

    CurrentUserView getById(Long id);

    /**
     * Upgrade-only email-verification sync: sets {@code email_verified=true} when the token reports
     * verified and the stored flag is still false. Never downgrades true→false. Returns the fresh view.
     */
    CurrentUserView syncEmailVerified(Long userId, boolean tokenEmailVerified);
}
