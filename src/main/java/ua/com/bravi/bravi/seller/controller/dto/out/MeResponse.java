package ua.com.bravi.bravi.seller.controller.dto.out;

import java.util.List;

public record MeResponse(
        MeUserResponse user,
        List<MeAccountResponse> accounts
) {

    public record MeUserResponse(
            String userId,
            String email,
            String firstName,
            String lastName,
            boolean emailVerified,
            String status
    ) {
    }

    public record MeAccountResponse(
            String accountId,
            String type,
            String role,
            String status,
            MeSellerResponse seller
    ) {
    }

    public record MeSellerResponse(
            String onboardingStatus
    ) {
    }
}
