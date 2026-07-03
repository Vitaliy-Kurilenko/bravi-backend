package ua.com.bravi.bravi.seller.controller.dto.out;

public record SellerRegistrationResponse(
        String userId,
        String accountId,
        String membershipId,
        String accountType,
        String accountStatus,
        String onboardingStatus
) {
}
