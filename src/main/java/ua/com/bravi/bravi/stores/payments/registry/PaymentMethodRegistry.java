package ua.com.bravi.bravi.stores.payments.registry;

import org.springframework.stereotype.Component;
import ua.com.bravi.bravi.stores.payments.exception.UnknownPaymentMethodException;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PaymentMethodRegistry {

    private final Map<String, PaymentMethodProvider> providers;

    public PaymentMethodRegistry(List<PaymentMethodProvider> providers) {
        this.providers = providers.stream().collect(Collectors.toMap(
                PaymentMethodProvider::code,
                Function.identity(),
                (existing, duplicate) -> {
                    throw new IllegalStateException("Duplicate payment method code: " + existing.code());
                },
                LinkedHashMap::new));
    }

    public Collection<PaymentMethodProvider> all() {
        return providers.values();
    }

    public PaymentMethodProvider get(String code) {
        PaymentMethodProvider provider = providers.get(code);
        if (provider == null) {
            throw new UnknownPaymentMethodException(code);
        }
        return provider;
    }
}
