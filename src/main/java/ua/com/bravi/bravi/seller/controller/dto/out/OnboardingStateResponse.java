package ua.com.bravi.bravi.seller.controller.dto.out;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Onboarding progress snapshot for {@code GET /accounts/{id}/seller/onboarding}. */
public record OnboardingStateResponse(
        @JsonProperty("account_status")
        String accountStatus,
        @JsonProperty("onboarding_status")
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
