package ua.com.bravi.bravi.seller.stores.api.event;

import java.time.Instant;

public record StoreCreatedEvent(
        Long storeId,
        Long sellerAccountId,
        Instant at
) {
}
