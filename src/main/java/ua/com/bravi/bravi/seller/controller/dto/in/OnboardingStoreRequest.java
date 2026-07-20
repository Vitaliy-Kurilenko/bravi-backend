package ua.com.bravi.bravi.seller.controller.dto.in;

import jakarta.validation.constraints.NotBlank;

/** Body for creating the DRAFT store during onboarding (POST). Logo is uploaded separately. */
public record OnboardingStoreRequest(
        @NotBlank
        String name,
        String description,
        String country
) {
}
