package ua.com.bravi.bravi.orders.domain;

import java.time.Instant;
import java.util.Map;

public record OrderShipment(
        Long id,
        String carrierCode,
        String trackingNumber,
        String deliveryStatus,
        Map<String, String> rawPayload,
        Instant createdAt,
        Instant updatedAt
) {
}
