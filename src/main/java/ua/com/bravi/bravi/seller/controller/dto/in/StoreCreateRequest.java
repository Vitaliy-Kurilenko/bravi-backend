package ua.com.bravi.bravi.seller.controller.dto.in;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ua.com.bravi.bravi.seller.stores.domain.WorkingHours;

import java.time.ZoneId;
import java.util.Currency;

public record StoreCreateRequest(
        @NotBlank
        String name,
        String description,
        String country,
        String region,
        String city,
        @JsonProperty("postal_code")
        String postalCode,
        String address,
        @JsonProperty("address_additional")
        String addressAdditional,
        @NotNull
        ZoneId timezone,
        @JsonProperty("logo_url")
        String logoUrl,
        @JsonProperty("working_hours")
        WorkingHours workingHours,
        @NotNull
        Currency currency,
        @JsonProperty("allow_return")
        @NotNull
        Boolean allowReturn
) {
}
