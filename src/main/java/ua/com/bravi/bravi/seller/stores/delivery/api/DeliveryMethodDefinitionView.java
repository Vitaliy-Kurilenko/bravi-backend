package ua.com.bravi.bravi.seller.stores.delivery.api;

import java.util.List;

public record DeliveryMethodDefinitionView(
        String code,
        String displayName,
        List<ConfigFieldView> configSchema
) {
}
