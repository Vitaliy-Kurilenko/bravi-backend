package ua.com.bravi.bravi.stores.delivery.registry;

import org.springframework.stereotype.Component;
import ua.com.bravi.bravi.stores.delivery.exception.UnknownDeliveryMethodException;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class DeliveryMethodRegistry {

    private final Map<String, DeliveryMethodProvider> providers;

    public DeliveryMethodRegistry(List<DeliveryMethodProvider> providers) {
        this.providers = providers.stream().collect(Collectors.toMap(
                DeliveryMethodProvider::code,
                Function.identity(),
                (existing, duplicate) -> {
                    throw new IllegalStateException("Duplicate delivery method code: " + existing.code());
                },
                LinkedHashMap::new));
    }

    public Collection<DeliveryMethodProvider> all() {
        return providers.values();
    }

    public DeliveryMethodProvider get(String code) {
        DeliveryMethodProvider provider = providers.get(code);
        if (provider == null) {
            throw new UnknownDeliveryMethodException(code);
        }
        return provider;
    }
}
