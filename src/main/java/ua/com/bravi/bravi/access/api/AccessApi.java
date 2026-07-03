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

    /** Accounts the current user belongs to (active memberships), each with the user's role codes. */
    List<AccountMembershipView> findAccountMembershipsByCurrentUser();

    Optional<AccountView> findAccountById(Long id);

    /** The account + owner membership of the given type owned by the user, if any (explicit userId; no request context). */
    Optional<OwnerAccountView> findOwnerAccount(Long userId, String accountType);

    /**
     * Provisions a new account of the given type (e.g. {@code "SELLER"}) owned by the given user:
     * creates the account (PENDING_ONBOARDING), an ACTIVE membership for the user, and assigns the
     * system {@code <TYPE>_OWNER} role. Returns the created account + membership.
     */
    OwnerAccountView provisionOwnerAccount(Long userId, String accountType);

    /** True when the current account grants the given permission code (e.g. {@code STORE_WRITE}). */
    boolean currentUserHasPermission(String permissionCode);
}
