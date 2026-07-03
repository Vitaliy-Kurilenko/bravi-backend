package ua.com.bravi.bravi.seller.controller.dto.in;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Body for patching the DRAFT store during onboarding (PATCH); null fields are left unchanged. */
public record OnboardingStorePatchRequest(
        String name,
        String description,
        @JsonProperty("logo_url")
        String logoUrl
) {
}
