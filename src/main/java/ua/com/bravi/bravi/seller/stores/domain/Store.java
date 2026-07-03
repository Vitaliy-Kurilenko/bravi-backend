package ua.com.bravi.bravi.seller.stores.domain;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Currency;

public record Store(
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
        StoreStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
