package ua.com.bravi.bravi.seller.controller.dto.in;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/** Body for creating the DRAFT store during onboarding (POST). */
public record OnboardingStoreRequest(
        @NotBlank
        String name,
        String description,
        @JsonProperty("logo_url")
        String logoUrl
) {
}
