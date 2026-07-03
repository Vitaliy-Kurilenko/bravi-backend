package ua.com.bravi.bravi.seller.account.api;

public record SellerAccountView(
        Long accountId,
        String accountPublicId,
        String legalName,
        String onboardingStatus,
        String contactEmail,
        String phone
) {
}
