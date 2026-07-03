package ua.com.bravi.bravi.access.api;

/**
 * Result of provisioning an owner account: the created account plus the owner membership,
 * with both internal ids and public ids (the latter surfaced by callers in API responses).
 */
public record OwnerAccountView(
        Long accountId,
        String accountPublicId,
        String accountType,
        String accountStatus,
        Long membershipId,
        String membershipPublicId
) {
}
