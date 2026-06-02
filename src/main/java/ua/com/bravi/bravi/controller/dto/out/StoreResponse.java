package ua.com.bravi.bravi.controller.dto.out;

import com.fasterxml.jackson.annotation.JsonProperty;
import ua.com.bravi.bravi.domain.store.WorkingHours;

import java.time.ZoneId;
import java.util.Currency;

public record StoreResponse(
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
        @JsonProperty("logo_url")
        String logoUrl,
        @JsonProperty("working_hours")
        WorkingHours workingHours,
        Currency currency,
        @JsonProperty("allow_return")
        Boolean allowReturn,
        String status
) {
}
