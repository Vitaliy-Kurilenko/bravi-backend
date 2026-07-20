package ua.com.bravi.bravi.seller.controller.dto.in;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZoneId;
import java.util.Currency;
import java.util.Locale;

/** Body for patching store settings during onboarding; null fields are left unchanged. */
public record OnboardingSettingsRequest(
        @JsonProperty("default_currency")
        Currency defaultCurrency,
        @JsonProperty("default_language")
        Locale defaultLanguage,
        @JsonProperty("default_weight_unit")
        String defaultWeightUnit,
        @JsonProperty("default_dimension_unit")
        String defaultDimensionUnit,
        ZoneId timezone
) {
}
