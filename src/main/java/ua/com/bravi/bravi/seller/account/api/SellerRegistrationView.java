package ua.com.bravi.bravi.seller.account.api;

/** Public registration context created (or found) for a seller. All ids are public ids. */
public record SellerRegistrationView(
        String userId,
        String accountId,
        String membershipId,
        String accountType,
        String accountStatus,
        String onboardingStatus
) {
}
