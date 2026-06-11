# Додавання нового методу доставки

Додавання методу доставки — це **один файл**: новий `@Component`, що реалізує
`DeliveryMethodProvider`. Спільний код, контролер, сервіс, реєстр і схему БД чіпати **не треба**.

Модуль: `ua.com.bravi.bravi.stores.delivery`. Провайдери живуть у
`stores/delivery/registry/providers/`.

---

## Крок 1. Створити провайдер

Новий клас, що реалізує `DeliveryMethodProvider` і позначений `@Component`. Spring автоматично
підхопить його, а `DeliveryMethodRegistry` зареєструє за `code()`.

**Приклад — Нова Пошта з трьома параметрами конфігурації:**

```java
package ua.com.bravi.bravi.stores.delivery.registry.providers;

import org.springframework.stereotype.Component;
import ua.com.bravi.bravi.stores.delivery.api.ConfigFieldView;
import ua.com.bravi.bravi.stores.delivery.exception.InvalidDeliveryConfigException;
import ua.com.bravi.bravi.stores.delivery.registry.DeliveryMethodProvider;

import java.util.List;
import java.util.Map;

@Component
public class NovaPoshtaProvider implements DeliveryMethodProvider {

    @Override
    public String code() {
        return "NOVA_POSHTA";            // унікальний стабільний ключ
    }

    @Override
    public String displayName() {
        return "Нова Пошта";             // людська назва для UI
    }

    @Override
    public List<ConfigFieldView> configSchema() {
        return List.of(
                new ConfigFieldView("apiKey", "API ключ", true, "PASSWORD"),
                new ConfigFieldView("senderRef", "Ref відправника", true, "TEXT"),
                new ConfigFieldView("defaultCity", "Місто за замовчуванням", false, "TEXT")
        );
    }

    // Опціонально — лише якщо потрібна валідація понад «обов'язкове + непорожнє».
    @Override
    public void validateConfig(Map<String, String> config) {
        DeliveryMethodProvider.super.validateConfig(config);   // спершу базова перевірка required

        String apiKey = config.get("apiKey");
        if (apiKey != null && apiKey.length() < 32) {
            throw new InvalidDeliveryConfigException("apiKey",
                    "API ключ має бути не коротшим за 32 символи");
        }
    }
}
```

Якщо метод не потребує конфігу — повертай `List.of()` у `configSchema()` (як
`SelfPickupProvider`), і `validateConfig` можна не перевизначати.

---

## Крок 2. Що дає кожен елемент контракту

| Елемент | Призначення |
|---|---|
| `code()` | Унікальний ідентифікатор. Іде в URL (`PUT /seller/stores/delivery/NOVA_POSHTA`) та в колонку `method_code`. **Дублікат коду → застосунок падає на старті** (`DeliveryMethodRegistry` кидає `IllegalStateException`). |
| `displayName()` | Назва для фронтенду в каталозі `/available`. |
| `configSchema()` | Декларація полів конфігу: `ConfigFieldView(key, label, required, type)`. Фронт за нею будує форму. |
| `validateConfig(config)` | **Дефолт уже** перевіряє, що всі `required`-поля присутні й непорожні (інакше `422`). Перевизначай **тільки** для складніших правил. |

Поле `type` (`"TEXT"`, `"PASSWORD"`, `"NUMBER"` …) — підказка для UI; бекенд його не інтерпретує,
зберігання завжди `String`. Тримай набір значень узгодженим із фронтом.

---

## Крок 3. (За потреби) юніт-тест провайдера

Швидкий тест без Spring — за зразком `DeliveryMethodRegistryTest`:

```java
@Test
void novaPoshtaRequiresApiKey() {
    DeliveryMethodProvider novaPoshta = new NovaPoshtaProvider();

    assertThatThrownBy(() -> novaPoshta.validateConfig(Map.of()))
            .isInstanceOf(InvalidDeliveryConfigException.class);

    novaPoshta.validateConfig(Map.of(
            "apiKey", "a".repeat(32),
            "senderRef", "ref-123"));     // не кидає
}
```

---

## Крок 4. Готово — нічого більше

Після додавання класу метод **одразу**:

- з'являється в каталозі `GET /seller/stores/delivery/available` (разом зі своєю `config_schema`);
- стає підключуваним: `PUT /seller/stores/delivery/NOVA_POSHTA` з тілом
  `{"config": {"apiKey": "...", "senderRef": "..."}}`;
- редагується (`PATCH`), вимикається (`DELETE`, конфіг зберігається).

---

## Як це працює під капотом

1. `DeliveryService.enableMethod` → `registry.get(code)` → нема коду → `UnknownDeliveryMethodException`
   → **404**.
2. → `provider.validateConfig(config)` → невалідно → `InvalidDeliveryConfigException(field, message)`
   → **422** (`UNPROCESSABLE_CONTENT`) у форматі `FiledValidationError`.
3. Конфіг лягає у JSONB-колонку `config` як є — **схему БД міняти не треба**, які б поля ти не оголосив.

## Чого робити НЕ треба

- ❌ Правити `DeliveryMethodRegistry`, `DeliveryService`, `SellerDeliveryController`, `DeliveryDtoMapper`
- ❌ Додавати enum чи Flyway-міграцію
- ❌ Чіпати `DeliveryApi` чи будь-який спільний код

У цьому й суть розширюваності: одна точка розширення — `@Component implements DeliveryMethodProvider`.

> ⚠️ Якщо новий провайдер зберігає секрети (API-ключі, токени) — пам'ятай, що зараз конфіг
> повертається в `GET`-відповідях **як є, без маскування**. Це усвідомлений поточний non-goal;
> якщо потрібно приховувати — це окрема задача (прапорець `secret` у `ConfigFieldView` +
> маскування у мапінгу відповіді).

---

## Методи оплати

Механізм методів оплати (`stores.payments`) ідентичний: реалізуй
`PaymentMethodProvider` (`code` / `displayName` / `configSchema` / `validateConfig`) як
`@Component` у `stores/payments/registry/providers/`. Ендпоінти — під `/seller/stores/payments`.
Деталі — у [`adding-payment-method.md`](adding-payment-method.md).