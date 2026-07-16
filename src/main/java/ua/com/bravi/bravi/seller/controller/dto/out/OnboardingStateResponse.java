package ua.com.bravi.bravi.seller.controller.dto.out;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/** Onboarding progress snapshot for {@code GET /accounts/{id}/seller/onboarding}. */
public record OnboardingStateResponse(
        @JsonProperty("account_status")
        @Schema(allowableValues = {"PENDING_ONBOARDING", "ACTIVE", "BLOCKED", "DISABLED"})
        String accountStatus,
        @JsonProperty("onboarding_status")
        @Schema(allowableValues = {"NOT_STARTED", "IN_PROGRESS", "COMPLETED"})
        String onboardingStatus,
        OnboardingStepsResponse steps,
        StoreResponse store
) {

    /** Per-step completion flags. */
    public record OnboardingStepsResponse(
            boolean store,
            boolean settings,
            boolean contacts
    ) {
    }
}
