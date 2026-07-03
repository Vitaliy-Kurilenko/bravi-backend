package ua.com.bravi.bravi.seller.controller.dto.in;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** Body for replacing the store's contacts during onboarding (PUT). */
public record OnboardingContactsRequest(
        @NotNull
        @Valid
        List<StoreContactCreateRequest> contacts
) {
}
