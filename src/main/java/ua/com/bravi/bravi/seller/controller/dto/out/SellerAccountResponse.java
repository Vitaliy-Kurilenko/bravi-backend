package ua.com.bravi.bravi.seller.controller.dto.out;

public record SellerAccountResponse(
        Long accountId,
        String accountPublicId,
        String legalName,
        String onboardingStatus,
        String contactEmail,
        String phone
) {
}
