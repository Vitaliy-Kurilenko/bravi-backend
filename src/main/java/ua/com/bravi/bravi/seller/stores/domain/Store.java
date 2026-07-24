package ua.com.bravi.bravi.seller.stores.domain;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Currency;
import java.util.Locale;

public record Store(
        Long id,
        Long sellerAccountId,
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
        Locale language,
        String weightUnit,
        String dimensionUnit,
        Boolean allowReturn,
        StoreStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
