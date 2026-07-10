package ua.com.bravi.bravi.seller.controller.dto.out;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record AccountsResponse(
        UserResponse user,
        List<AccountItemResponse> accounts
) {

    public record UserResponse(
            @JsonProperty("user_id")
            String userId,
            String email,
            @JsonProperty("first_name")
            String firstName,
            @JsonProperty("last_name")
            String lastName,
            @JsonProperty("email_verified")
            boolean emailVerified,
            String status
    ) {
    }

    public record AccountItemResponse(
            @JsonProperty("account_id")
            String accountId,
            String type,
            String role,
            String status,
            SellerResponse seller
    ) {
    }

    public record SellerResponse(
            @JsonProperty("onboarding_status")
            String onboardingStatus
    ) {
    }
}
