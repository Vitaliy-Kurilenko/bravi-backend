package ua.com.bravi.bravi.seller.stores.delivery.registry;

import org.junit.jupiter.api.Test;
import ua.com.bravi.bravi.seller.stores.delivery.api.ConfigFieldView;
import ua.com.bravi.bravi.seller.stores.delivery.exception.InvalidDeliveryConfigException;
import ua.com.bravi.bravi.seller.stores.delivery.exception.UnknownDeliveryMethodException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeliveryMethodRegistryTest {

    private static DeliveryMethodProvider provider(String code, List<ConfigFieldView> schema) {
        return new DeliveryMethodProvider() {
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
        List<DeliveryMethodProvider> providers = List.of(
                provider("SELF_PICKUP", List.of()),
                provider("SELF_PICKUP", List.of()));

        assertThatThrownBy(() -> new DeliveryMethodRegistry(providers))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SELF_PICKUP");
    }

    @Test
    void getReturnsRegisteredProvider() {
        DeliveryMethodProvider pickup = provider("SELF_PICKUP", List.of());
        DeliveryMethodRegistry registry = new DeliveryMethodRegistry(List.of(pickup));

        assertThat(registry.get("SELF_PICKUP")).isSameAs(pickup);
        assertThat(registry.all()).containsExactly(pickup);
    }

    @Test
    void getThrowsForUnknownCode() {
        DeliveryMethodRegistry registry = new DeliveryMethodRegistry(List.of(provider("SELF_PICKUP", List.of())));

        assertThatThrownBy(() -> registry.get("NOVA_POSHTA"))
                .isInstanceOf(UnknownDeliveryMethodException.class);
    }

    @Test
    void defaultValidationFailsWhenRequiredFieldMissing() {
        DeliveryMethodProvider withApiKey = provider("NOVA_POSHTA",
                List.of(new ConfigFieldView("apiKey", "API key", true, "TEXT")));

        assertThatThrownBy(() -> withApiKey.validateConfig(Map.of()))
                .isInstanceOf(InvalidDeliveryConfigException.class)
                .extracting("field").isEqualTo("apiKey");
    }

    @Test
    void defaultValidationPassesWhenRequiredFieldPresent() {
        DeliveryMethodProvider withApiKey = provider("NOVA_POSHTA",
                List.of(new ConfigFieldView("apiKey", "API key", true, "TEXT")));

        withApiKey.validateConfig(Map.of("apiKey", "secret"));
    }
}
