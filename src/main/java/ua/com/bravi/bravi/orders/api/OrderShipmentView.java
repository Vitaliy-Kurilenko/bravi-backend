package ua.com.bravi.bravi.orders.api;

import java.time.Instant;
import java.util.Map;

public record OrderShipmentView(
        Long id,
        String carrierCode,
        String trackingNumber,
        String deliveryStatus,
        Map<String, String> rawPayload,
        Instant createdAt,
        Instant updatedAt
) {
}
