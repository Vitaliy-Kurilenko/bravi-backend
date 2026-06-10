package ua.com.bravi.bravi.stores.api;

import ua.com.bravi.bravi.stores.domain.WorkingHours;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Currency;

public record StoreView(
        Long id,
        Long sellerId,
        String name,
        String description,
        String country,
        String region,
        String city,
        String postalCode,
        String address,
        String addressAdditional,
        ZoneId timezone,
        String logoUrl,
        WorkingHours workingHours,
        Currency currency,
        Boolean allowReturn,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
