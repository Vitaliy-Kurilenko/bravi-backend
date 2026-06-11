package ua.com.bravi.bravi.stores.delivery.registry.providers;

import org.springframework.stereotype.Component;
import ua.com.bravi.bravi.stores.delivery.api.ConfigFieldView;
import ua.com.bravi.bravi.stores.delivery.registry.DeliveryMethodProvider;

import java.util.List;

@Component
public class SelfPickupProvider implements DeliveryMethodProvider {

    @Override
    public String code() {
        return "SELF_PICKUP";
    }

    @Override
    public String displayName() {
        return "Self pickup";
    }

    @Override
    public List<ConfigFieldView> configSchema() {
        return List.of();
    }
}
