package ua.com.bravi.bravi.stores.payments.api;

import java.time.Instant;
import java.util.Map;

public record StorePaymentMethodView(
        Long id,
        Long storeId,
        String methodCode,
        boolean enabled,
        Map<String, String> config,
        Instant createdAt,
        Instant updatedAt
) {
}
