package ua.com.bravi.bravi.seller.stores.payments.registry.providers;

import org.springframework.stereotype.Component;
import ua.com.bravi.bravi.seller.stores.payments.api.ConfigFieldView;
import ua.com.bravi.bravi.seller.stores.payments.registry.PaymentMethodProvider;

import java.util.List;

@Component
public class CashOnDeliveryProvider implements PaymentMethodProvider {

    @Override
    public String code() {
        return "CASH_ON_DELIVERY";
    }

    @Override
    public String displayName() {
        return "Cash on delivery";
    }

    @Override
    public List<ConfigFieldView> configSchema() {
        return List.of();
    }
}
