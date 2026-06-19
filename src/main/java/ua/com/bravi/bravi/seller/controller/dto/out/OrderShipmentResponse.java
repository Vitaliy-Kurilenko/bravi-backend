package ua.com.bravi.bravi.seller.controller.dto.out;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Map;

public record OrderShipmentResponse(
        Long id,
        @JsonProperty("carrier_code")
        String carrierCode,
        @JsonProperty("tracking_number")
        String trackingNumber,
        @JsonProperty("delivery_status")
        String deliveryStatus,
        @JsonProperty("raw_payload")
        Map<String, String> rawPayload,
        @JsonProperty("created_at")
        Instant createdAt,
        @JsonProperty("updated_at")
        Instant updatedAt
) {
}
