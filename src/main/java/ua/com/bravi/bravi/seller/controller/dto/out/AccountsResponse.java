package ua.com.bravi.bravi.seller.controller.dto.out;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record AccountsResponse(
        AccountUserResponse user,
        List<AccountItemResponse> accounts
) {

    public record AccountUserResponse(
            @JsonProperty("user_id")
            String userId,
            String email,
            @JsonProperty("first_name")
            String firstName,
            @JsonProperty("last_name")
            String lastName,
            @JsonProperty("email_verified")
            boolean emailVerified,
            @Schema(allowableValues = {"PENDING_ACTIVATION", "ACTIVE", "BLOCKED"})
            String status
    ) {
    }

    public record AccountItemResponse(
            @JsonProperty("account_id")
            String accountId,
            @Schema(allowableValues = {"SELLER", "SUPPLIER"})
            String type,
            @Schema(allowableValues = {"SELLER_OWNER", "SELLER_MEMBER"})
            String role,
            @Schema(allowableValues = {"PENDING_ONBOARDING", "ACTIVE", "BLOCKED", "DISABLED"})
            String status,
            SellerResponse seller
    ) {
    }

    public record SellerResponse(
            @JsonProperty("onboarding_status")
            @Schema(allowableValues = {"NOT_STARTED", "IN_PROGRESS", "COMPLETED"})
            String onboardingStatus
    ) {
    }
}
