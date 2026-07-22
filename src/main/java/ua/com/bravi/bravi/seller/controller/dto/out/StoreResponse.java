package ua.com.bravi.bravi.seller.controller.dto.out;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import ua.com.bravi.bravi.seller.stores.domain.WorkingHours;

import java.time.ZoneId;
import java.util.Currency;

public record StoreResponse(
        @JsonProperty("public_id")
        String publicId,
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
        @Schema(allowableValues = {"DRAFT", "ACTIVE", "DISABLED", "ARCHIVED"})
        String status
) {
}
