package ua.com.bravi.bravi.seller.stores.payments.registry;

import ua.com.bravi.bravi.seller.stores.payments.api.ConfigFieldView;
import ua.com.bravi.bravi.seller.stores.payments.exception.InvalidPaymentConfigException;

import java.util.List;
import java.util.Map;

public interface PaymentMethodProvider {

    String code();

    String displayName();

    List<ConfigFieldView> configSchema();

    default void validateConfig(Map<String, String> config) {
        for (ConfigFieldView field : configSchema()) {
            if (field.required()) {
                String value = config == null ? null : config.get(field.key());
                if (value == null || value.isBlank()) {
                    throw new InvalidPaymentConfigException(field.key(),
                            "Field '" + field.key() + "' is required");
                }
            }
        }
    }
}
