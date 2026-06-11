package ua.com.bravi.bravi.stores.payments.api;

import java.util.List;

public record PaymentMethodDefinitionView(
        String code,
        String displayName,
        List<ConfigFieldView> configSchema
) {
}
