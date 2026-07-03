package ua.com.bravi.bravi.seller.account.api;

import java.util.UUID;

/**
 * Input for provisioning a seller's business context from an external registration
 * (Auth Service). Idempotent by {@code keycloakUserId}.
 */
public record SellerRegistrationCommand(
        UUID keycloakUserId,
        String email,
        String firstName,
        String lastName,
        String idempotencyKey
) {
}
