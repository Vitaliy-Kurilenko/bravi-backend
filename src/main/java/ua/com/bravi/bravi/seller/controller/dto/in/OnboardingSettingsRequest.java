package ua.com.bravi.bravi.seller.controller.dto.in;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Body for patching store settings during onboarding; null fields are left unchanged. */
public record OnboardingSettingsRequest(
        @JsonProperty("default_currency")
        String defaultCurrency,
        @JsonProperty("default_language")
        String defaultLanguage,
        @JsonProperty("default_weight_unit")
        String defaultWeightUnit,
        @JsonProperty("default_dimension_unit")
        String defaultDimensionUnit,
        String timezone
) {
}
