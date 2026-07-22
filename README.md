# Bravi

Backend-застосунок на **Spring Boot 4.x / Java 26**, PostgreSQL, інтеграція з Keycloak.
Правила та конвенції розробки — у [CLAUDE.md](CLAUDE.md).

## Загальний опис

Проект — **Spring Modulith**-моноліт: код поділено на бізнес-модулі в базовому пакеті
`ua.com.bravi.bravi`, а межі між ними enforce'яться Spring Modulith (`@ApplicationModule`,
`@NamedInterface`). Модулі спілкуються лише через опубліковані named interface'и (`api`) та
доменні події; цілісність меж перевіряється тестом `ModulithStructureTest`
(`ApplicationModules.verify()`).

Ключові модулі:

- **`shared`** — крос-катінг інфраструктура (фільтри, `SecurityConfig`, `InvocationContext`,
  базові винятки, утиліти). OPEN-модуль, доступний усім.
- **`identity`** — користувачі. Явний провіженінг: користувач створюється лише під час реєстрації
  (Keycloak `sub` → `ext_id`), не JIT. `GET /accounts` (authenticated) резолвить користувача
  lookup-only, синхронізує `email_verified` з JWT (false→true) і повертає його акаунти з ролями та
  onboarding-статусом; невідомий користувач → 404.
- **`access`** — tenancy + RBAC: акаунти, членства (memberships), ролі та права (permissions).
  Резолвить поточний акаунт користувача та його права.
- **`dictionaries`** — системні довідники (валюти, мови, одиниці ваги/габаритів, часові пояси,
  країни)
  у парі таблиць `dictionaries` + `dictionary_items` (тип-специфічні атрибути — JSONB `meta`),
  наповнюються Flyway-seed'ом, керуватимуться майбутньою адмін-панеллю. Універсальний read-API
  для фронтів: `GET /dictionaries` (перелік довідників) і `GET /dictionaries/{code}` (активні
  елементи, відсортовані за `sort_order`; невідомий код → 404). Доступ — будь-який
  автентифікований користувач (`bravi_user`).
- **`seller`** — вертикаль продавця: реєстрація seller-акаунта, онбординг, магазини,
  товари/категорії/виробники магазину, замовлення продавця. Day-to-day REST **скоупиться магазином
  у шляху**: `/stores/{storePublicId}/**` (`/categories`, `/manufacturers`, `/products`, `/orders`,
  `/contacts`, а сам магазин — корінь `/stores/{storePublicId}`). Акаунт **виводиться з магазину**
  (`stores.seller_account_id`), окремо в URL не передається. Онбординг (магазину ще нема) —
  `/accounts/{accountPublicId}/seller/onboarding/**`. Перелік магазинів акаунта —
  `GET /accounts/{accountPublicId}/seller/stores` (account-scoped, `hasPermission('STORE','READ')`).
  `SellerContextInterceptor` резолвить магазин зі
  шляху, виводить акаунт і перевіряє ACTIVE membership користувача (невідомий/чужий магазин → 404;
  для онбординг-акаунта без membership → 403) — перед `@PreAuthorize hasPermission(...)`.
  Жодного «першого акаунта/магазину».

**Реєстрація:** зовнішній Auth Service створює користувача в Keycloak і викликає внутрішній
`POST /internal/registrations/seller`, який створює бізнес-контекст (User + Account + SellerAccount +
Membership, ідемпотентно за `keycloakUserId`). Цей ендпоінт захищений — токен має нести роль
`service_registration` (Keycloak service-account Auth-сервісу).

**Онбординг продавця** (після реєстрації + верифікації email, `role_seller`): фронт веде
користувача через `/accounts/{accountId}/seller/onboarding` —
`GET` (стан), `POST /store` (створює DRAFT-магазин + дефолтні settings + manual sales-channel,
onboarding → IN_PROGRESS), `PATCH /store`, `PATCH /store/settings`, `PUT /store/contacts`,
`POST /complete` (перевіряє `email_verified`, наявність магазину й manual-каналу, далі
account → ACTIVE, onboarding → COMPLETED, store → ACTIVE). Один магазин на seller-акаунт.

**Мультитенантність:** користувач належить до акаунта через membership; seller-акаунт має
рівно один магазин.

**Аутентифікація:** Spring Security OAuth2 resource server — валідація JWT Keycloak через JWKS;
ролі беруться з claim'а `realm_access.roles`. Формат помилок — RFC 9457 `ProblemDetail`.

**Персистентність:** JPA/Hibernate + PostgreSQL; схема керується **Flyway**
(`src/main/resources/db.migration/`, застосовується автоматично при старті). Документація API —
Swagger UI / OpenAPI (springdoc).

## Вимоги

- JDK 26
- PostgreSQL
- Запущений Keycloak (для аутентифікації)
- Docker — для інтеграційних тестів (Testcontainers)

## Конфігурація — змінні оточення

Обов'язкові (без них застосунок не стартує):

| Змінна        | Опис                    | Приклад                                  |
|---------------|-------------------------|------------------------------------------|
| `DB_URL`      | JDBC-URL до PostgreSQL  | `jdbc:postgresql://localhost:5432/bravi` |
| `DB_USER`     | Користувач БД           | `bravi`                                  |
| `DB_PASSWORD` | Пароль користувача БД   | `secret`                                 |

Опціональні (мають дефолти в `application.yaml`, перевизначаються через env завдяки relaxed binding):

| Змінна                | Опис                                               | Дефолт                  |
|-----------------------|----------------------------------------------------|-------------------------|
| `KEYCLOACK_BASE_URL`  | Базовий URL Keycloak (для збірки JWT `issuer-uri`) | `http://localhost:8080` |
| `KEYCLOACK_REALM`     | Realm Keycloak                                     | `bravi`                 |
| `KEYCLOACK_CLIENT_ID` | Client ID застосунку в Keycloak                    | `user-token-proxy`      |
| `INTERNAL_API_ROLE`   | Realm-роль для доступу до `/internal/**`            | `service_registration`  |
| `SERVER_PORT`         | Порт HTTP-сервера                                  | `8083`                  |
| `PRODUCT_IMAGE_PATH`  | Каталог локального сховища фото товарів            | `./data/product-images` |
| `MEDIA_S3_ENDPOINT`   | Endpoint об'єктного сховища (S3/MinIO)             | `http://localhost:9000` |
| `MEDIA_S3_REGION`     | Регіон сховища                                     | `us-east-1`             |
| `MEDIA_S3_BUCKET`     | Bucket для медіа (логотипи магазинів)              | `bravi-media`           |
| `MEDIA_S3_ACCESS_KEY` | Access key сховища                                 | `minioadmin`            |
| `MEDIA_S3_SECRET_KEY` | Secret key сховища                                 | `minioadmin`            |
| `MEDIA_S3_PATH_STYLE` | Path-style-доступ (`true` для MinIO)               | `true`                  |
| `MEDIA_PUBLIC_BASE_URL` | База публічних URL медіа-об'єктів                | `http://localhost:9000/bravi-media` |

> JWT `issuer-uri` збирається як `${KEYCLOACK_BASE_URL}/realms/${KEYCLOACK_REALM}`.
> Секрети (`DB_PASSWORD`, `MEDIA_S3_SECRET_KEY` тощо) не комітяться — лише через env або зовнішній vault.
> Дефолти `minioadmin/minioadmin` — це кореневі креденшали локального MinIO, не для проду.

### Об'єктне сховище (логотипи магазинів)

Логотип магазину завантажується не на backend, а **напряму в S3/MinIO** за presigned-посиланням:
`POST .../store/logo/upload-url` (backend валідує тип/розмір і видає presigned PUT URL) → клієнт
`PUT`-ить файл у сховище → `PATCH .../store { "logo_storage_key": "<key>" }` чіпляє об'єкт до магазину
(backend звіряє існування об'єкта, власника, розмір і прибирає старий). `logo_url` у магазині —
стабільне публічне посилання на об'єкт. Видалення — `DELETE .../store/logo`. Один bucket, розкладка за
префіксом ключа централізована в `shared/media/MediaCategory` (лого → `store-logos/{storeId}/…`).

Локально MinIO піднімається через `docker-compose.yml` (сервіс `minio` + одноразовий `minio-init`,
який створює bucket `bravi-media` і вмикає анонімне читання):

```bash
docker compose up -d
```

- API: `http://localhost:9000`, консоль: `http://localhost:9001` (логін/пароль — `minioadmin`
  за замовчуванням або значення `MEDIA_S3_ACCESS_KEY` / `MEDIA_S3_SECRET_KEY`, якщо задані в `.env`).
- Дані зберігаються у volume `minio-data` (переживає `docker compose down`; `down -v` — скидає).

Базовий контекст-шлях API: `/api` (наприклад, `http://localhost:8083/api`).

## Запуск

```bash
export DB_URL=jdbc:postgresql://localhost:5432/bravi
export DB_USER=bravi
export DB_PASSWORD=secret

./mvnw spring-boot:run
```

Flyway-міграції застосовуються автоматично при старті.

### Зовнішній вхід через API-gateway

У проді/локальному стеку клієнти не ходять на `8083` напряму, а через **nginx API-gateway**
(`http://localhost/api/...`), який фронтить `bravi` та `auth-service` за одним портом. Gateway живе
в docker-compose auth-репо (`../auth/docker-compose.yml`, сервіс `gateway`); `bravi` при цьому
піднімається окремо з IDE на `8083`, і gateway дістає його через `host.docker.internal:8083`.
Маршрутизація за шляхом: `/api/auth/**`, `/api/registration/**` → auth-service; решта `/api/**` →
bravi. Деталі — у README auth-репо.

## Тести

```bash
./mvnw clean verify
```

Інтеграційні тести підіймають реальний PostgreSQL через Testcontainers (потрібен Docker);
unit-тести доменної/інфра-логіки виконуються без Spring-контексту.

Тести з суфіксом `*IT` (напр. `S3MediaStorageIT`, який піднімає MinIO) запускає **failsafe**
у фазі `verify` — `./mvnw test` їх НЕ виконує, тільки `./mvnw verify`.
