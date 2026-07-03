package ua.com.bravi.bravi.seller.account.api;

/** Input for onboarding a new seller account for the current user. */
public record SellerAccountRegistration(
        String legalName,
        String contactEmail,
        String phone
) {
}
