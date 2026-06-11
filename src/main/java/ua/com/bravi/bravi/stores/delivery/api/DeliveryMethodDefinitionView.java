package ua.com.bravi.bravi.stores.delivery.api;

import java.util.List;

public record DeliveryMethodDefinitionView(
        String code,
        String displayName,
        List<ConfigFieldView> configSchema
) {
}
