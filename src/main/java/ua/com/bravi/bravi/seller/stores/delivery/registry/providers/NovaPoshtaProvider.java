package ua.com.bravi.bravi.seller.stores.delivery.registry.providers;

import org.springframework.stereotype.Component;
import ua.com.bravi.bravi.seller.stores.delivery.api.ConfigFieldView;
import ua.com.bravi.bravi.seller.stores.delivery.exception.InvalidDeliveryConfigException;
import ua.com.bravi.bravi.seller.stores.delivery.registry.DeliveryMethodProvider;

import java.util.List;
import java.util.Map;

// Додано тимчасово, буде розширено в майбутньому
@Component
public class NovaPoshtaProvider implements DeliveryMethodProvider {
@Override
      public String code() {
          return "NOVA_POSHTA";
      }

      @Override
      public String displayName() {
          return "Нова Пошта";
      }

      @Override
      public List<ConfigFieldView> configSchema() {
          return List.of(
                  new ConfigFieldView("apiKey", "API ключ", true, "PASSWORD"),
                  new ConfigFieldView("senderRef", "Ref відправника", true, "TEXT"),
                  new ConfigFieldView("defaultCity", "Місто за замовчуванням", false, "TEXT")
          );
      }

      @Override
      public void validateConfig(Map<String, String> config) {
          DeliveryMethodProvider.super.validateConfig(config);   // спершу базова перевірка required

          String apiKey = config.get("apiKey");
          if (apiKey != null && apiKey.length() < 32) {
              throw new InvalidDeliveryConfigException("apiKey", "API ключ має бути не коротшим за 32 символи");
          }
      }

}
