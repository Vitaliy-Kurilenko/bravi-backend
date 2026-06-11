package ua.com.bravi.bravi.seller.controller.dto.out;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Map;

public record StoreDeliveryMethodResponse(
        Long id,
        @JsonProperty("method_code")
        String methodCode,
        boolean enabled,
        Map<String, String> config,
        @JsonProperty("created_at")
        Instant createdAt,
        @JsonProperty("updated_at")
        Instant updatedAt
) {
}
