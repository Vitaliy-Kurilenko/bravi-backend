package ua.com.bravi.bravi.stores.payments.registry;

import org.junit.jupiter.api.Test;
import ua.com.bravi.bravi.stores.payments.api.ConfigFieldView;
import ua.com.bravi.bravi.stores.payments.exception.InvalidPaymentConfigException;
import ua.com.bravi.bravi.stores.payments.exception.UnknownPaymentMethodException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentMethodRegistryTest {

    private static PaymentMethodProvider provider(String code, List<ConfigFieldView> schema) {
        return new PaymentMethodProvider() {
            @Override
            public String code() {
                return code;
            }

            @Override
            public String displayName() {
                return code;
            }

            @Override
            public List<ConfigFieldView> configSchema() {
                return schema;
            }
        };
    }

    @Test
    void rejectsDuplicateCodesOnConstruction() {
        List<PaymentMethodProvider> providers = List.of(
                provider("CASH_ON_DELIVERY", List.of()),
                provider("CASH_ON_DELIVERY", List.of()));

        assertThatThrownBy(() -> new PaymentMethodRegistry(providers))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CASH_ON_DELIVERY");
    }

    @Test
    void getReturnsRegisteredProvider() {
        PaymentMethodProvider cash = provider("CASH_ON_DELIVERY", List.of());
        PaymentMethodRegistry registry = new PaymentMethodRegistry(List.of(cash));

        assertThat(registry.get("CASH_ON_DELIVERY")).isSameAs(cash);
        assertThat(registry.all()).containsExactly(cash);
    }

    @Test
    void getThrowsForUnknownCode() {
        PaymentMethodRegistry registry = new PaymentMethodRegistry(List.of(provider("CASH_ON_DELIVERY", List.of())));

        assertThatThrownBy(() -> registry.get("STRIPE"))
                .isInstanceOf(UnknownPaymentMethodException.class);
    }

    @Test
    void defaultValidationFailsWhenRequiredFieldMissing() {
        PaymentMethodProvider withSecret = provider("STRIPE",
                List.of(new ConfigFieldView("secretKey", "Secret key", true, "PASSWORD")));

        assertThatThrownBy(() -> withSecret.validateConfig(Map.of()))
                .isInstanceOf(InvalidPaymentConfigException.class)
                .extracting("field").isEqualTo("secretKey");
    }

    @Test
    void defaultValidationPassesWhenRequiredFieldPresent() {
        PaymentMethodProvider withSecret = provider("STRIPE",
                List.of(new ConfigFieldView("secretKey", "Secret key", true, "PASSWORD")));

        withSecret.validateConfig(Map.of("secretKey", "sk_live_123"));
    }
}
