package ua.com.bravi.bravi.stores.delivery.registry;

import ua.com.bravi.bravi.stores.delivery.api.ConfigFieldView;
import ua.com.bravi.bravi.stores.delivery.exception.InvalidDeliveryConfigException;

import java.util.List;
import java.util.Map;

public interface DeliveryMethodProvider {

    String code();

    String displayName();

    List<ConfigFieldView> configSchema();

    default void validateConfig(Map<String, String> config) {
        for (ConfigFieldView field : configSchema()) {
            if (field.required()) {
                String value = config == null ? null : config.get(field.key());
                if (value == null || value.isBlank()) {
                    throw new InvalidDeliveryConfigException(field.key(),
                            "Field '" + field.key() + "' is required");
                }
            }
        }
    }
}
