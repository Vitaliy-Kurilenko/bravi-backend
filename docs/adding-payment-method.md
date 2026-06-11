# Додавання нового методу оплати

Додавання методу оплати — це **один файл**: новий `@Component`, що реалізує
`PaymentMethodProvider`. Спільний код, контролер, сервіс, реєстр і схему БД чіпати **не треба**.

Модуль: `ua.com.bravi.bravi.stores.payments`. Провайдери живуть у
`stores/payments/registry/providers/`.

---

## Крок 1. Створити провайдер

Новий клас, що реалізує `PaymentMethodProvider` і позначений `@Component`. Spring автоматично
підхопить його, а `PaymentMethodRegistry` зареєструє за `code()`.

**Приклад — LiqPay із двома параметрами конфігурації:**

```java
package ua.com.bravi.bravi.stores.payments.registry.providers;

import org.springframework.stereotype.Component;
import ua.com.bravi.bravi.stores.payments.api.ConfigFieldView;
import ua.com.bravi.bravi.stores.payments.exception.InvalidPaymentConfigException;
import ua.com.bravi.bravi.stores.payments.registry.PaymentMethodProvider;

import java.util.List;
import java.util.Map;

@Component
public class LiqPayProvider implements PaymentMethodProvider {

    @Override
    public String code() {
        return "LIQPAY";                 // унікальний стабільний ключ
    }

    @Override
    public String displayName() {
        return "LiqPay";                 // людська назва для UI
    }

    @Override
    public List<ConfigFieldView> configSchema() {
        return List.of(
                new ConfigFieldView("publicKey", "Публічний ключ", true, "TEXT"),
                new ConfigFieldView("privateKey", "Приватний ключ", true, "PASSWORD")
        );
    }

    // Опціонально — лише якщо потрібна валідація понад «обов'язкове + непорожнє».
    @Override
    public void validateConfig(Map<String, String> config) {
        PaymentMethodProvider.super.validateConfig(config);   // спершу базова перевірка required

        String publicKey = config.get("publicKey");
        if (publicKey != null && !publicKey.startsWith("i")) {
            throw new InvalidPaymentConfigException("publicKey",
                    "Публічний ключ LiqPay має починатися з 'i'");
        }
    }
}
```

Якщо метод не потребує конфігу — повертай `List.of()` у `configSchema()` (як
`CashOnDeliveryProvider`), і `validateConfig` можна не перевизначати.

---

## Крок 2. Що дає кожен елемент контракту

| Елемент | Призначення |
|---|---|
| `code()` | Унікальний ідентифікатор. Іде в URL (`PUT /seller/stores/payments/LIQPAY`) та в колонку `method_code`. **Дублікат коду → застосунок падає на старті** (`PaymentMethodRegistry` кидає `IllegalStateException`). |
| `displayName()` | Назва для фронтенду в каталозі `/available`. |
| `configSchema()` | Декларація полів конфігу: `ConfigFieldView(key, label, required, type)`. Фронт за нею будує форму. |
| `validateConfig(config)` | **Дефолт уже** перевіряє, що всі `required`-поля присутні й непорожні (інакше `422`). Перевизначай **тільки** для складніших правил. |

Поле `type` (`"TEXT"`, `"PASSWORD"`, `"NUMBER"` …) — підказка для UI; бекенд його не інтерпретує,
зберігання завжди `String`. Тримай набір значень узгодженим із фронтом.

---

## Крок 3. (За потреби) юніт-тест провайдера

Швидкий тест без Spring — за зразком `PaymentMethodRegistryTest`:

```java
@Test
void liqPayRequiresKeys() {
    PaymentMethodProvider liqPay = new LiqPayProvider();

    assertThatThrownBy(() -> liqPay.validateConfig(Map.of()))
            .isInstanceOf(InvalidPaymentConfigException.class);

    liqPay.validateConfig(Map.of("publicKey", "i123", "privateKey", "secret"));  // не кидає
}
```

---

## Крок 4. Готово — нічого більше

Після додавання класу метод **одразу**:

- з'являється в каталозі `GET /seller/stores/payments/available` (разом зі своєю `config_schema`);
- стає підключуваним: `PUT /seller/stores/payments/LIQPAY` з тілом
  `{"config": {"publicKey": "...", "privateKey": "..."}}`;
- редагується (`PATCH`), вимикається (`DELETE`, конфіг зберігається).

---

## Як це працює під капотом

1. `PaymentService.enableMethod` → `registry.get(code)` → нема коду → `UnknownPaymentMethodException`
   → **404**.
2. → `provider.validateConfig(config)` → невалідно → `InvalidPaymentConfigException(field, message)`
   → **422** (`UNPROCESSABLE_CONTENT`) у форматі `FiledValidationError`.
3. Конфіг лягає у JSONB-колонку `config` як є — **схему БД міняти не треба**, які б поля ти не оголосив.

## Чого робити НЕ треба

- ❌ Правити `PaymentMethodRegistry`, `PaymentService`, `SellerPaymentController`, `PaymentDtoMapper`
- ❌ Додавати enum чи Flyway-міграцію
- ❌ Чіпати `PaymentsApi` чи будь-який спільний код

У цьому й суть розширюваності: одна точка розширення — `@Component implements PaymentMethodProvider`.

> ⚠️ Якщо новий провайдер зберігає секрети (приватні ключі, токени) — пам'ятай, що зараз конфіг
> повертається в `GET`-відповідях **як є, без маскування**. Це усвідомлений поточний non-goal;
> якщо потрібно приховувати — це окрема задача (прапорець `secret` у `ConfigFieldView` +
> маскування у мапінгу відповіді).

---

## Методи доставки

Механізм методів доставки (`stores.delivery`) ідентичний: реалізуй
`DeliveryMethodProvider` (`code` / `displayName` / `configSchema` / `validateConfig`) як
`@Component` у `stores/delivery/registry/providers/`. Ендпоінти — під `/seller/stores/delivery`.