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
├── catalog/        ← Manufacturer (manufacturers/) + Category (categories/) + Product (products/) + ManufacturersApi/CategoriesApi/ProductsApi; решта — заготовка
├── orders/         ← Order + OrderItem + OrderShipment (order_statuses довідник) + OrdersApi
├── seller/         ← REST-контролери з префіксом /seller/** (hasAuthority('SELLER'))
└── buyer/          ← REST-контролери з префіксом /buyer/** (hasAuthority('BUYER'))
```

**Міжмодульний зв'язок:** seller/buyer звертаються до resource-модулів лише через named interfaces `users::api`, `stores::api`, `catalog::api`, `orders::api` (інтерфейси + view records). Сам модуль `orders` компонує `users::api`, `catalog::api`, `stores::api` (товари/покупець/методи оплати-доставки). Подійні нотифікації — через Spring Application Events (`UserProvisionedEvent`, `StoreCreatedEvent`); таблиця `event_publication` (Modulith JPA event registry) ведеться Flyway-міграцією `V4`. Цілісність модульних меж перевіряється `ModulithStructureTest`.

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
| GET | `/seller/manufacturers` | Усі виробники магазину поточного селлера | `200` `ManufacturerResponse[]`; `404` якщо магазину нема |
| GET | `/seller/manufacturers/{manufacturerId}` | Виробник магазину (лише власник) | `200` `ManufacturerResponse`; `404`/`403` |
| POST | `/seller/manufacturers` | Створити виробника | `201`; `404` нема магазину; `409` дубль назви; `400` валідація |
| PATCH | `/seller/manufacturers/{manufacturerId}` | Часткове оновлення виробника (лише власник) | `204`; `404`/`403`/`409` |
| DELETE | `/seller/manufacturers/{manufacturerId}` | Видалити виробника (лише власник) | `204`; `404`/`403` |
| GET | `/seller/categories` | Дерево категорій магазину поточного селлера | `200` `CategoryResponse[]` (вкладене); `404` якщо магазину нема |
| GET | `/seller/categories/{categoryId}` | Піддерево категорії (лише власник) | `200` `CategoryResponse`; `404`/`403` |
| POST | `/seller/categories` | Створити категорію (опц. `parent_id`) | `201`; `404` нема магазину/parent; `409` дубль назви; `400` валідація/глибина |
| PATCH | `/seller/categories/{categoryId}` | Часткове оновлення + опц. переміщення (`parent_id`) | `204`; `404`/`403`/`409`; `400` цикл/глибина |
| DELETE | `/seller/categories/{categoryId}` | Видалити категорію (лише власник) | `204`; `404`/`403`; `409` якщо має підкатегорії |
| GET | `/seller/products` | Список товарів: пошук/фільтр/пагінація/сортування | `200` `ProductPageResponse` |
| GET | `/seller/products/{productId}` | Товар магазину (лише власник) | `200` `ProductResponse`; `404`/`403` |
| POST | `/seller/products` | Створити товар | `201` `ProductResponse`; `409` дубль code/sku; `404` невідомий category/manufacturer/stock; `400` валідація |
| PATCH | `/seller/products/{productId}` | Часткове оновлення товару | `204`; `404`/`403`/`409`/`400` |
| DELETE | `/seller/products/{productId}` | Видалити товар разом із фото | `204`; `404`/`403` |
| GET | `/seller/products/{productId}/images` | Галерея фото товару | `200` `ProductImageResponse[]` |
| POST | `/seller/products/{productId}/images` | Завантажити фото (multipart `file`, опц. `is_primary`) | `201` `ProductImageResponse`; `400` не-image/завеликий |
| GET | `/seller/products/{productId}/images/{imageId}` | Бінарний вміст фото | `200` (image/*); `404` |
| PUT | `/seller/products/{productId}/images/{imageId}` | Замінити файл фото (multipart `file`) | `200` `ProductImageResponse`; `400` |
| DELETE | `/seller/products/{productId}/images/{imageId}` | Видалити фото | `204`; `404` |
| GET | `/seller/orders` | Список замовлень: пошук/фільтр/пагінація/сортування | `200` `OrderPageResponse` |
| GET | `/seller/orders/statuses` | Довідник статусів замовлень | `200` `OrderStatusResponse[]` |
| GET | `/seller/orders/{orderId}` | Замовлення магазину (лише власник) | `200` `OrderResponse`; `404`/`403` |
| POST | `/seller/orders` | Створити замовлення | `201` `OrderResponse`; `400` невідомий buyer/метод/товар/валідація |
| PATCH | `/seller/orders/{orderId}` | Часткове оновлення (статус, отримувач, доставка, коментарі) | `200` `OrderResponse`; `404`/`403`/`400` |
| DELETE | `/seller/orders/{orderId}` | Видалити замовлення разом із позиціями | `204`; `404`/`403` |
| POST | `/seller/orders/{orderId}/items` | Додати позицію (товар) у замовлення | `201` `OrderResponse`; `404`/`403`; `400` невідомий товар |
| PATCH | `/seller/orders/{orderId}/items/{itemId}` | Змінити кількість/ціну або замінити товар позиції | `200` `OrderResponse`; `404`/`403`/`400` |
| DELETE | `/seller/orders/{orderId}/items/{itemId}` | Видалити позицію (≥1 має лишитись) | `200` `OrderResponse`; `404`/`403`; `400` остання позиція |

Спроба BUYER'а звернутись на `/seller/**` (або навпаки) — `403 Forbidden`.

Магазин прив'язаний до користувача один-до-одного (`stores.seller_id` UNIQUE). `status` магазину — серверно-кероване (дефолт `ACTIVE`), з клієнта не приймається; на PATCH оновлюються тільки передані поля.

Контакти прив'язані до магазину один-до-багатьох (`store_contacts.store_id`). Підтримувані типи (`ContactType`): `PHONE`, `EMAIL`, `WEBSITE`, `VIBER`, `WHATSAPP`, `TELEGRAM`. Значення `value` валідуються відповідно до типу (email-формат, URL зі схемою `http`/`https`, телефонний номер, `@username` для Telegram). Редагувати/видаляти можна лише контакти власного магазину — інакше `403`.

Методи доставки (`store_delivery_methods.store_id`) — один-до-багатьох на магазин, унікальні в межах магазину за `method_code`. Набір реалізованих методів — це plugin-провайдери в коді (`DeliveryMethodProvider`); додавання нового методу зводиться до одного `@Component` без правок спільного коду чи схеми БД (наразі є приклад `SELF_PICKUP`). Конфіг методу зберігається гнучко (JSONB `config`), валідується відповідним провайдером. `PUT` підключає+вмикає метод (ідемпотентний upsert), `DELETE` лише вимикає (`enabled=false`), не втрачаючи конфіг.

Методи оплати (`store_payment_methods.store_id`) влаштовані ідентично до методів доставки: plugin-провайдери (`PaymentMethodProvider`), реєстр із перевіркою унікальності кодів на старті, гнучкий JSONB-конфіг із валідацією провайдером, та сама upsert/disable семантика. Додати новий метод = один `@Component` (наразі є приклад `CASH_ON_DELIVERY`). Конфіг повертається у відповідях як є — секрети платіжних провайдерів наразі не маскуються.

Виробники (`manufacturers.store_id`) — один-до-багатьох на магазин (під-модуль `catalog/manufacturers/`), на них надалі посилатимуться товари. Назва (`name`) обов'язкова й унікальна в межах магазину (`uq_manufacturers_store_name`); спроба дубля — `409`. Статус (`ManufacturerStatus`: `ACTIVE`/`INACTIVE`) приймається з клієнта в тілі POST/PATCH; при створенні без `status` — дефолт `ACTIVE`, на PATCH `status` оновлюється лише якщо переданий; невалідне значення — `400`. Переглядати/редагувати/видаляти можна лише виробників власного магазину — інакше `403`/`404`.

Товари (`products.store_id`, під-модуль `catalog/products/`) — створюються seller'ом у межах магазину. `code` обов'язковий і унікальний у магазині (`uq_products_store_code` → `409`); `sku` опційний, унікальний-якщо-заданий (частковий індекс). Прив'язки опційні й валідуються по магазину: `category_id` через `CategoriesApi`, `manufacturer_id` через `ManufacturersApi` (чужий/неіснуючий → `404`/`403`). Статус наявності — довідник `stock_statuses` (seed: `IN_STOCK`/`OUT_OF_STOCK`/`PREORDER`), `stock_status_id` обов'язковий FK. Ціни (`partner_price`, `recommended_price`) — `BigDecimal` у валюті магазину; `status` (`ProductStatus` `ACTIVE`/`INACTIVE`, дефолт `ACTIVE`). `GET /seller/products` підтримує `search` (по name/code/sku), фільтри (`category_ids`, `manufacturer_ids`, `stock_statuses`, `statuses`, `min_price`/`max_price` по partner_price, `created_from`/`created_to`), сортування (`sort_by` ∈ id/name/price/recommended_price/quantity/stock_status_id/created_at/updated_at/manufacturer_id, `sort_order` ASC/DESC) та пагінацію (`page` від 1, `limit` ≤ 100); відповідь — `{data, count_per_page, count, limit, pages, page, sort_by, sort_order}`.

**Фото товару** (`product_images`, 1:N, галерея) — завантаження/заміна/видалення через `/seller/products/{id}/images`. Сховище за портом `ProductImageStorage` із локальним FS-адаптером (`LocalProductImageStorage`); у БД — лише `storage_key` + метадані. Перше фото стає головним (`is_primary`); видалення головного підвищує наступне. Приймаються лише `image/*` у межах ліміту розміру. Шлях сховища — env `PRODUCT_IMAGE_PATH` (дефолт `./data/product-images`); заміна на S3/MinIO — новий адаптер порту без змін викликачів.

Категорії товарів (`categories.store_id`, під-модуль `catalog/categories/`) — **ієрархічне дерево до 3 рівнів** у межах магазину; батько задається через `parent_id` (self-FK `fk_categories_on_parent`). Назва обов'язкова й унікальна **серед сіблінгів** (окремі часткові індекси для коренів `uq_categories_root_name` та підкатегорій `uq_categories_child_name`); дубль — `409`. Статус (`CategoryStatus`: `ACTIVE`/`INACTIVE`) — як у виробника (дефолт `ACTIVE`). Інваріанти дерева (`CategoryHierarchyPolicy`, домен): глибина ≤ 3, заборона циклів. **Переміщення** виконується в рамках `PATCH`: `parent_id` non-null переміщує піддерево під вказаного батька (перевірка циклу/глибини → `400`); `parent_id` null/відсутній — батько не змінюється (від'єднання назад у корінь через PATCH не підтримується). `GET` повертає вкладене дерево (`children[]`). Видалення категорії з підкатегоріями блокується — `409`. Усі дії — лише в межах власного магазину (`403`/`404`).

Замовлення (`orders.store_id`, окремий бізнес-модуль `orders/`) — створюються seller'ом у межах магазину, прив'язані до покупця (`buyer_id` — користувач типу `BUYER`, валідується через `UsersApi`) і до статусу з **довідника `order_statuses`** (seed: `NEW`/`PROCESSING`/`SHIPPED`/`DELIVERED`/`CANCELLED`; таблиця розширювана адміністратором). При створенні статус — `NEW`. `payment_method_code`/`delivery_method_code` валідуються проти **увімкнених** методів магазину (`PaymentsApi`/`DeliveryApi`); невідомий buyer/метод/товар → `400` з `FiledValidationError`. Адреса доставки — структуровані колонки + дискримінатор `delivery_type` (`ADDRESS`/`WAREHOUSE`): для дому — `delivery_country/region/city/address/extra`, для відділення — `delivery_warehouse_no`. Дані поштової служби (`carrier_code`, `tracking_number`, `delivery_status`, JSONB `raw_payload`) винесені в окрему сутність `order_shipments` (1:1) — схема та read-проєкція (`shipment` у `GET`) уже є, але **заповнення винесено в окрему задачу** (реальна інтеграція з карʼєром); наразі через API не пишеться. Позиції замовлення (`order_items`, 1:N) — снапшот товару на момент створення (`sku`, `code`, `name`, `partner_price` з `ProductsApi`; `sale_price` з запиту або `recommended_price` за замовчуванням). Позиції редагуються окремими sub-resource ендпоінтами `/seller/orders/{id}/items[/{itemId}]`: додати товар (`POST`), змінити кількість/`sale_price` чи **замінити товар** позиції (`PATCH` — при заміні sku/code/name/partner_price реснапшотяться з нового товару, `sale_price` за замовч. = його `recommended_price`), видалити позицію (`DELETE`). Замовлення завжди має ≥1 позицію — видалення останньої → `400`. Після кожної зміни позицій `subtotal`/`total` перераховуються бекендом. **Суми рахує бекенд** (`OrderTotals`, домен): `subtotal` = Σ `sale_price`×`quantity`, `total` = `subtotal` − `discount_total` + `shipping_total`; клієнт передає лише `discount_total`/`shipping_total`/`prepayment`. `internal_comment` — внутрішній коментар для seller'а. Зміна статусу й даних отримувача/доставки виконується в рамках `PATCH` ресурсу (не окремим ендпоінтом). `GET /seller/orders` підтримує `search` (по телефону/ПІБ/email отримувача), фільтри (`buyer_ids`, `payment_method_codes`, `delivery_method_codes`, `recipient_name`, `recipient_phone`, `recipient_email`, `statuses` за кодами, `min_total`/`max_total` по total, `created_from`/`created_to`), сортування (`sort_by` ∈ id/status/recipient_name/shipping_method_code/payment_method_code/partner_id/created_at, `sort_order` ASC/DESC) та пагінацію (`page` від 1, `limit` ≤ 100); відповідь — `{data, count_per_page, count, limit, pages, page, sort_by, sort_order}`. Усі дії — лише в межах власного магазину (`403`/`404`).

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
| `PRODUCT_IMAGE_PATH`  | Каталог локального сховища фото товарів           | `./data/product-images` |

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
