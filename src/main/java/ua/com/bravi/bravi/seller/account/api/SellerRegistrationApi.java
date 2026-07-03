package ua.com.bravi.bravi.seller.account.api;

public interface SellerRegistrationApi {

    /**
     * Provisions the business context for a newly registered seller: User + SELLER Account
     * (PENDING_ONBOARDING) + SellerAccount (NOT_STARTED) + owner Membership. Idempotent by
     * {@code keycloakUserId} — a repeat call returns the existing context.
     */
    SellerRegistrationView register(SellerRegistrationCommand command);
}
