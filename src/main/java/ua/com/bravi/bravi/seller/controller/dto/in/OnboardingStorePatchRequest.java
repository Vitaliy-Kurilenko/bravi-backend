package ua.com.bravi.bravi.seller.controller.dto.in;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Body for patching the DRAFT store during onboarding (PATCH); null fields are left unchanged.
 * A non-null {@code logo_storage_key} (from the presign upload-url step) attaches the uploaded logo.
 */
public record OnboardingStorePatchRequest(
        String name,
        String description,
        String country,
        @JsonProperty("logo_storage_key")
        String logoStorageKey
) {
}
