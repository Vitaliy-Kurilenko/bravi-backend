package ua.com.bravi.bravi.access.api;

import java.util.List;

/**
 * Resolved authorization context for the current user's active account:
 * which account is in scope and the role/permission codes granted through it.
 */
public record AccessContextView(
        Long accountId,
        String accountPublicId,
        String accountType,
        List<String> roleCodes,
        List<String> permissionCodes
) {
}
