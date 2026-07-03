package ua.com.bravi.bravi.access.api;

import java.util.List;

/** An account the current user belongs to, with the user's role codes in that account. */
public record AccountMembershipView(
        Long accountId,
        String accountPublicId,
        String type,
        String status,
        List<String> roleCodes
) {
}
