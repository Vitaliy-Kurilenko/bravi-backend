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
     * Authorization context for the given account (by public id) and the current user.
     * Resolved from {@code InvocationContext.userId}. Empty when the account does not
     * exist or the user has no ACTIVE membership on it — the caller renders 403.
     */
    Optional<AccessContextView> resolveContext(String accountPublicId);

    /** Same as {@link #resolveContext(String)} but for an account already resolved to its internal id
     *  (e.g. derived from a store). Empty when the user has no ACTIVE membership on it. */
    Optional<AccessContextView> resolveContext(Long accountId);

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

    /** Transitions an account to ACTIVE (seller onboarding completion). */
    void activateAccount(Long accountId);
}
