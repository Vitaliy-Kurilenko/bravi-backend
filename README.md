# Bravi

Backend-застосунок на **Spring Boot 4.x / Java 26**, PostgreSQL, інтеграція з Keycloak.
Деталі конвенцій розробки — у [CLAUDE.md](CLAUDE.md).

## Архітектура

Проект організовано як **Spring Modulith**-моноліт. Кожен модуль — підпакет в `ua.com.bravi.bravi`:

```
ua.com.bravi.bravi
├── shared/         ← OPEN-модуль: фільтри, SecurityConfig, InvocationContext, базові винятки
├── users/          ← User domain/persistence/service + UsersApi + UserProvisionedEvent
├── stores/         ← Store + StoreContact (contacts/) + StoreDeliveryMethod (delivery/) + StorePaymentMethod (payments/) + StoresApi/StoreContactsApi/DeliveryApi/PaymentsApi + StoreCreatedEvent
├── catalog/        ← заготовка
├── orders/         ← заготовка
├── seller/         ← REST-контролери з префіксом /seller/** (hasAuthority('SELLER'))
└── buyer/          ← REST-контролери з префіксом /buyer/** (hasAuthority('BUYER'))
```

**Міжмодульний зв'язок:** seller/buyer звертаються до resource-модулів лише через named interfaces `users::api`, `stores::api` (інтерфейси + view records). Подійні нотифікації — через Spring Application Events (`UserProvisionedEvent`, `StoreCreatedEvent`); таблиця `event_publication` (Modulith JPA event registry) ведеться Flyway-міграцією `V4`. Цілісність модульних меж перевіряється `ModulithStructureTest`.

## Функціонал

- **REST API** з рольовими префіксами `/seller/**`, `/buyer/**`, спільний `/users/**`; формат помилок — RFC 9457 `ProblemDetail`.
- **Аутентифікація через Keycloak** — Spring Security OAuth2 resource server, валідація JWT через JWKS; ролі беруться з claim'у `realm_access.roles`.
- **Cross-cutting HTTP-ланцюжок фільтрів:**
  - `RequiredHeadersFilter` — валідує наявність обов'язкових заголовків (`X-Correlation-Id`), повертає `400 ProblemDetail` за відсутності;
  - `RequestIdMdcFilter` — кладе `requestId` у MDC (для кореляції логів) та echo'їть у response;
  - `InvocationContextFilter` — наповнює `@RequestScope` `InvocationContext` (користувач, ролі, девайс) з JWT та заголовків;
  - `UserAgentParser` — розбір `User-Agent` (yauaa) у `DeviceInfo`.
- **Розрізнення помилок JWT** — окремі відповіді для протермінованого (`token_expired`) та невалідного (`token_invalid`) токена через `ProblemDetailAuthenticationEntryPoint`.
- **Just-in-time provisioning користувача** — на кожен автентифікований запит `CurrentUserInterceptor` бере `ext_id` (Keycloak `sub`) з `InvocationContext`, шукає користувача в `users` і, якщо його немає, автоматично створює (`status=ACTIVE`, `type` з JWT-claim'а `user_type`). Внутрішній `id` користувача та його дані записуються в `InvocationContext` для downstream-логіки. Споживані JWT-claim'и: `user_type` (`BUYER`/`SELLER`), `given_name`/`family_name` (fallback `firstName` на `preferred_username`). Якщо для нового користувача `user_type` відсутній/невалідний — `422 User provisioning failed`.
- **Персистентність** — JPA/Hibernate + PostgreSQL, схема керується **Flyway** (`src/main/resources/db.migration/`).
- **Документація API** — Swagger UI / OpenAPI (springdoc).

## API ендпоінти

Усі під `/api` (context-path), потребують валідного JWT і заголовка `X-Correlation-Id`. Поточний користувач визначається з токена (через `InvocationContext`), без id у шляху. Авторизація за ролями виконується Spring Security: `/seller/**` потребує `SELLER`, `/buyer/**` — `BUYER` (claim `realm_access.roles` JWT).

| Метод | Шлях | Опис | Відповідь |
|-------|------|------|-----------|
| GET | `/users/context` | Контекст поточного користувача (доступно SELLER та BUYER) | `200` `UserResponse` |
| GET | `/seller/stores` | Магазин поточного селлера | `200` `StoreResponse`; `404` якщо нема |
| POST | `/seller/stores` | Створити магазин | `201`; `409` якщо вже є; `400` валідація |
| PATCH | `/seller/stores` | Часткове оновлення магазину | `204`; `404`/`400` |
| GET | `/seller/stores/contacts` | Усі контакти магазину поточного селлера | `200` `StoreContactResponse[]`; `404` якщо магазину нема |
| POST | `/seller/stores/contacts` | Додати один або декілька контактів | `201`; `404` нема магазину; `400` валідація |
| PATCH | `/seller/stores/contacts/{contactId}` | Часткове оновлення контакту (лише власник) | `204`; `404`/`403`/`400` |
| DELETE | `/seller/stores/contacts/{contactId}` | Видалити контакт (лише власник) | `204`; `404`/`403` |
| GET | `/seller/stores/delivery/available` | Каталог реалізованих методів доставки | `200` `DeliveryMethodDefinitionResponse[]` |
| GET | `/seller/stores/delivery` | Методи доставки, підключені магазином | `200` `StoreDeliveryMethodResponse[]`; `404` якщо магазину нема |
| PUT | `/seller/stores/delivery/{methodCode}` | Підключити+налаштувати метод (ідемпотентно) | `204`; `404` невідомий код; `422` невалідний конфіг |
| PATCH | `/seller/stores/delivery/{methodCode}` | Оновити конфіг підключеного методу | `204`; `404` метод не підключено; `422` валідація |
| DELETE | `/seller/stores/delivery/{methodCode}` | Відключити метод (конфіг зберігається) | `204`; `404` метод не підключено |
| GET | `/seller/stores/payments/available` | Каталог реалізованих методів оплати | `200` `PaymentMethodDefinitionResponse[]` |
| GET | `/seller/stores/payments` | Методи оплати, підключені магазином | `200` `StorePaymentMethodResponse[]`; `404` якщо магазину нема |
| PUT | `/seller/stores/payments/{methodCode}` | Підключити+налаштувати метод (ідемпотентно) | `204`; `404` невідомий код; `422` невалідний конфіг |
| PATCH | `/seller/stores/payments/{methodCode}` | Оновити конфіг підключеного методу | `204`; `404` метод не підключено; `422` валідація |
| DELETE | `/seller/stores/payments/{methodCode}` | Відключити метод (конфіг зберігається) | `204`; `404` метод не підключено |

Спроба BUYER'а звернутись на `/seller/**` (або навпаки) — `403 Forbidden`.

Магазин прив'язаний до користувача один-до-одного (`stores.seller_id` UNIQUE). `status` магазину — серверно-кероване (дефолт `ACTIVE`), з клієнта не приймається; на PATCH оновлюються тільки передані поля.

Контакти прив'язані до магазину один-до-багатьох (`store_contacts.store_id`). Підтримувані типи (`ContactType`): `PHONE`, `EMAIL`, `WEBSITE`, `VIBER`, `WHATSAPP`, `TELEGRAM`. Значення `value` валідуються відповідно до типу (email-формат, URL зі схемою `http`/`https`, телефонний номер, `@username` для Telegram). Редагувати/видаляти можна лише контакти власного магазину — інакше `403`.

Методи доставки (`store_delivery_methods.store_id`) — один-до-багатьох на магазин, унікальні в межах магазину за `method_code`. Набір реалізованих методів — це plugin-провайдери в коді (`DeliveryMethodProvider`); додавання нового методу зводиться до одного `@Component` без правок спільного коду чи схеми БД (наразі є приклад `SELF_PICKUP`). Конфіг методу зберігається гнучко (JSONB `config`), валідується відповідним провайдером. `PUT` підключає+вмикає метод (ідемпотентний upsert), `DELETE` лише вимикає (`enabled=false`), не втрачаючи конфіг.

Методи оплати (`store_payment_methods.store_id`) влаштовані ідентично до методів доставки: plugin-провайдери (`PaymentMethodProvider`), реєстр із перевіркою унікальності кодів на старті, гнучкий JSONB-конфіг із валідацією провайдером, та сама upsert/disable семантика. Додати новий метод = один `@Component` (наразі є приклад `CASH_ON_DELIVERY`). Конфіг повертається у відповідях як є — секрети платіжних провайдерів наразі не маскуються.

## Вимоги

- JDK 26
- PostgreSQL
- Запущений Keycloak (для роботи аутентифікації)
- Docker — для інтеграційних тестів (Testcontainers)

## Конфігурація — змінні оточення

Обов'язкові (без них застосунок не стартує):

| Змінна        | Опис                                                        | Приклад                                  |
|---------------|-------------------------------------------------------------|------------------------------------------|
| `DB_URL`      | JDBC-URL до PostgreSQL                                       | `jdbc:postgresql://localhost:5432/bravi` |
| `DB_USER`     | Користувач БД                                               | `bravi`                                  |
| `DB_PASSWORD` | Пароль користувача БД                                       | `secret`                                 |

Опціональні (мають дефолти в `application.yaml`, перевизначаються через env завдяки relaxed binding):

| Змінна                | Опис                                              | Дефолт                  |
|-----------------------|---------------------------------------------------|-------------------------|
| `KEYCLOACK_BASE_URL`  | Базовий URL Keycloak (для збірки JWT `issuer-uri`)| `http://localhost:8080` |
| `KEYCLOACK_REALM`     | Realm Keycloak                                    | `bravi`                 |
| `KEYCLOACK_CLIENT_ID` | Client ID застосунку в Keycloak                   | `user-token-proxy`      |
| `SERVER_PORT`         | Порт HTTP-сервера                                 | `8083`                  |

> JWT `issuer-uri` збирається як `${KEYCLOACK_BASE_URL}/realms/${KEYCLOACK_REALM}`.
> Секрети (`DB_PASSWORD` тощо) не комітяться — лише через env або зовнішній vault.

Базовий контекст-шлях API: `/api` (наприклад, `http://localhost:8083/api`).

## Запуск

```bash
export DB_URL=jdbc:postgresql://localhost:5432/bravi
export DB_USER=bravi
export DB_PASSWORD=secret

./mvnw spring-boot:run
```

Flyway-міграції застосовуються автоматично при старті.

## Тести

```bash
./mvnw clean verify
```

Інтеграційні тести підіймають реальний PostgreSQL через Testcontainers (потрібен Docker); unit-тести доменної/інфра-логіки виконуються без Spring-контексту.
