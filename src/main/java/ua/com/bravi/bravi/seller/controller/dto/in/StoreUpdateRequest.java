package ua.com.bravi.bravi.seller.controller.dto.in;

import com.fasterxml.jackson.annotation.JsonProperty;
import ua.com.bravi.bravi.seller.stores.domain.WorkingHours;

import java.time.ZoneId;
import java.util.Currency;
import java.util.Locale;

public record StoreUpdateRequest(
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
        ZoneId timezone,
        @JsonProperty("working_hours")
        WorkingHours workingHours,
        Currency currency,
        Locale language,
        @JsonProperty("weight_unit")
        String weightUnit,
        @JsonProperty("dimension_unit")
        String dimensionUnit,
        @JsonProperty("allow_return")
        Boolean allowReturn,
        @JsonProperty("logo_storage_key")
        String logoStorageKey
) {
}
