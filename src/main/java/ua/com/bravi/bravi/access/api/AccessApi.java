package ua.com.bravi.bravi.access.api;

import java.util.List;
import java.util.Optional;

/**
 * Public contract of the Access Control module: resolves the authorization
 * context (account + roles + permissions) for the current request's user and
 * exposes account lookups. Account/membership mutation is added in later steps.
 */
public interface AccessApi {

    /**
     * Authorization context of the current user's active account, if any.
     * Resolved from {@code InvocationContext.userId}. Empty when the user has
     * no active membership.
     */
    Optional<AccessContextView> resolveCurrentContext();

    List<AccountView> findAccountsByCurrentUser();

    Optional<AccountView> findAccountById(Long id);

    /** True when the current account grants the given permission code (e.g. {@code STORE_WRITE}). */
    boolean currentUserHasPermission(String permissionCode);
}
