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
  товари/категорії/виробники магазину, замовлення продавця. Контекст **передається заголовками**,
  а не в шляху: `X-Account-Id` (публічний id акаунта, майже в усіх запитах) → `AccountContext`;
  `X-Store-Id` (публічний id магазину, лише де потрібно) → `StoreContext`. Усі seller-ендпоінти
  згруповано під `/sellers/**`. Сам ресурс «магазин» — **path-based REST**: перелік
  `GET /sellers/stores` (лише `X-Account-Id`), а конкретний магазин — `/sellers/stores/{storeId}`
  (get/patch + logo). Store-scoped суб-ресурси скоупляться магазином через заголовок `X-Store-Id`:
  `/sellers/products`, `/sellers/categories`, `/sellers/manufacturers`, `/sellers/orders`,
  `/sellers/contacts`. Онбординг (магазину ще нема) — `/sellers/onboarding/**` (лише `X-Account-Id`).
  Перелік акаунтів користувача (discovery) — `GET /accounts` (без заголовків контексту).
  `AccountContextInterceptor` (модуль `access`) резолвить `X-Account-Id` → `AccountContext` і
  перевіряє ACTIVE membership (немає доступу → 403); `StoreContextInterceptor` (модуль
  `seller.stores`) резолвить магазин у `StoreContext` — з path-параметра `{storeId}`, інакше з
  заголовка `X-Store-Id` — звіряючи належність акаунту (невідомий/чужий магазин → 404); обидва
  перед `@PreAuthorize hasPermission(...)`.
  Жодного «першого акаунта/магазину».

**Реєстрація:** зовнішній Auth Service створює користувача в Keycloak і викликає внутрішній
`POST /internal/registrations/seller`, який створює бізнес-контекст (User + Account + SellerAccount +
Membership, ідемпотентно за `keycloakUserId`). Цей ендпоінт захищений — токен має нести роль
`service_registration` (Keycloak service-account Auth-сервісу).

**Онбординг продавця** (після реєстрації + верифікації email, `role_seller`): фронт веде
користувача через `/sellers/onboarding` (акаунт — у заголовку `X-Account-Id`) —
`GET` (стан), `POST /store` (створює DRAFT-магазин + дефолтні settings + manual sales-channel,
onboarding → IN_PROGRESS), `PATCH /store`, `PATCH /store/settings`, `PUT /store/contacts`,
`POST /complete` (перевіряє `email_verified`, наявність магазину й manual-каналу, далі
account → ACTIVE, onboarding → COMPLETED, store → ACTIVE). Один магазин на seller-акаунт.

**Каталог продавця:** `GET /sellers/products` і `GET /sellers/products/{publicId}` віддають
категорію та виробника вкладеними об'єктами з public id і назвою:
`"category": { "id": "cat_…", "name": "Ноутбуки" }`, `"manufacturer": { "id": "mnf_…", "name": "Lenovo" }`
(`null`, якщо товар без категорії/виробника). На запис (`POST`/`PATCH`) і у фільтрах списку
(`category_ids`, `manufacturer_ids`) — як і раніше, самі public id.

**Мультитенантність:** користувач належить до акаунта через membership; seller-акаунт має
рівно один магазин.

**Аутентифікація:** Spring Security OAuth2 resource server — валідація JWT Keycloak через JWKS;
ролі беруться з claim'а `resource_access.backend-service.roles` (client-ролі `backend-service`).
Формат помилок — RFC 9457 `ProblemDetail`.

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
| `KEYCLOACK_CLIENT_ID` | Client ID застосунку в Keycloak                    | `backend-service`       |
| `INTERNAL_API_ROLE`   | Client-роль `backend-service` для `/internal/**`   | `auth_service`          |
| `USER_API_ROLE`       | Client-роль `backend-service` для людей-користувачів | `bravi_user`          |
| `SERVER_PORT`         | Порт HTTP-сервера                                  | `8083`                  |
| `MEDIA_S3_ENDPOINT`   | Endpoint об'єктного сховища (S3/MinIO)             | `http://localhost:9000` |
| `MEDIA_S3_REGION`     | Регіон сховища                                     | `us-east-1`             |
| `MEDIA_S3_BUCKET`     | Bucket для медіа (логотипи магазинів, фото товарів) | `bravi-media`          |
| `MEDIA_S3_ACCESS_KEY` | Access key сховища                                 | `minioadmin`            |
| `MEDIA_S3_SECRET_KEY` | Secret key сховища                                 | `minioadmin`            |
| `MEDIA_S3_PATH_STYLE` | Path-style-доступ (`true` для MinIO)               | `true`                  |
| `MEDIA_PUBLIC_BASE_URL` | База публічних URL медіа-об'єктів                | `http://localhost:9000/bravi-media` |
| `SPRING_PROFILES_ACTIVE` | Активний профіль (`local` / `prod`)             | *(немає — базовий конфіг)* |
| `ROOT_LOG_LEVEL`      | Рівень кореневого логера                           | `INFO` (`WARN` у `prod`) |
| `APP_LOG_LEVEL`       | Рівень логів застосунку (`ua.com.bravi.bravi`)     | `INFO`                  |
| `ACCESS_LOG_LEVEL`    | Рівень HTTP access-логу (`OFF` — вимкнути)         | `INFO`                  |
| `PAYLOAD_LOG_LEVEL`   | `DEBUG` вмикає лог тіл запитів/відповідей          | `INFO` (`DEBUG` у `local`) |
| `SERVICE_CALL_LOG_LEVEL` | `DEBUG` вмикає лог аргументів викликів сервісів | `INFO` (`DEBUG` у `local`) |
| `APP_VERSION`         | Версія сервісу в полі `service.version` (профіль `prod`) | `unknown`         |
| `APP_ENVIRONMENT`     | Середовище в полі `service.environment` (профіль `prod`) | `prod`            |

> JWT `issuer-uri` збирається як `${KEYCLOACK_BASE_URL}/realms/${KEYCLOACK_REALM}`.
> Секрети (`DB_PASSWORD`, `MEDIA_S3_SECRET_KEY` тощо) не комітяться — лише через env або зовнішній vault.
> Дефолти `minioadmin/minioadmin` — це кореневі креденшали локального MinIO, не для проду.

### Об'єктне сховище (логотипи магазинів, фото товарів)

Медіа завантажується не на backend, а **напряму в S3/MinIO** за presigned-посиланням. Логотип магазину:
`POST .../store/logo/upload-url` (backend валідує тип/розмір і видає presigned PUT URL) → клієнт
`PUT`-ить файл у сховище → `PATCH .../store { "logo_storage_key": "<key>" }` чіпляє об'єкт до магазину
(backend звіряє існування об'єкта, власника, розмір і прибирає старий). `logo_url` у магазині —
стабільне публічне посилання на об'єкт. Видалення — `DELETE .../store/logo`.

Фото товару — той самий потік для галереї: `POST /sellers/products/{publicId}/images/upload-url`
(presigned PUT URL) → клієнт `PUT`-ить файл → `POST /sellers/products/{publicId}/images
{ "storage_key": "<key>", "is_primary": true }` чіпляє об'єкт до товару (backend звіряє об'єкт і власника).
`PATCH .../images/{imageId} { "is_primary": true }` робить фото головним, `DELETE .../images/{imageId}` —
прибирає його. У відповіді кожне фото несе публічний `url` об'єкта. Один bucket, розкладка за префіксом
ключа централізована в `shared/media/MediaCategory` (лого → `store-logos/{storeId}/…`,
фото товару → `product-images/{storeId}/{productId}/…`).

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

./mvnw spring-boot:run                                      # базовий конфіг (тихий, текстовий)
./mvnw spring-boot:run -Dspring-boot.run.profiles=local      # розробка: DEBUG + SQL
```

Flyway-міграції застосовуються автоматично при старті.

## Логування

Логи пишуться **тільки в stdout** — збором і ротацією займається docker/k8s. Файлових
appender'ів немає, `logback-spring.xml` не використовується: формат керується профілями
через штатний structured logging Spring Boot.

| Профіль             | Формат                                    | Коли                    |
|---------------------|-------------------------------------------|-------------------------|
| *(базовий, без профілю)* | Текстовий, тихі рівні (`INFO`/`WARN`) | CI, тести               |
| `local`             | Текстовий + `DEBUG`, SQL Hibernate і binds | Локальна розробка       |
| `prod`              | **JSON (ECS)** — готовий до Loki/ELK       | `SPRING_PROFILES_ACTIVE=prod` |

**Контекст запиту (MDC).** Кожен рядок логу під час обробки запиту несе `requestId`
(із заголовка `X-Correlation-Id`, а якщо його немає — згенерований сервером і повернутий
у відповіді), а також `accountId` / `storeId` (із `X-Account-Id` / `X-Store-Id`) і `userExtId`
(`sub` з JWT). У текстовому форматі вони йдуть префіксом, у JSON — окремими полями.
MDC заповнюється **до** Spring Security, тож 401/403 теж корелюються.

```
16:00:04.352  INFO --- [bravi] [exec-1] [corr-123] [acc_1/str_2] ua.com.bravi.bravi.access : GET /api/dictionaries 200 10ms
```

**HTTP access-log.** `AccessLogFilter` пише один рядок на завершений запит
(метод, шлях, статус, тривалість) у логер `ua.com.bravi.bravi.access` — рівень окремий
(`ACCESS_LOG_LEVEL`, `OFF` вимикає). Шляхи з `HttpConstants.EXCLUDED_PATHS`
(actuator, swagger, `/error`) не логуються.

**Винятки.** Необроблені винятки (500) логуються `ERROR` **зі стектрейсом** у
`GlobalExceptionHandler`; очікувані доменні 4xx — `WARN`/`DEBUG` без стектрейсу.

**Параметри запитів/відповідей і викликів між сервісами.** Вимкнені за замовчуванням;
вмикаються профілем `local` або точково env-змінними:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local   # обидва логери → DEBUG

# або без профілю, точково:
PAYLOAD_LOG_LEVEL=DEBUG SERVICE_CALL_LOG_LEVEL=DEBUG ./mvnw spring-boot:run
```

```
--> POST /api/sellers/onboarding/store query=[] body=[{"name":"Olga's Shop"}]
<-- POST /api/sellers/onboarding/store status=201 body=[{"public_id":"st_IjWs…","status":"DRAFT"}]
--> UserService.provisionUser(keycloakUserId=5555…, email=***, firstName=***, lastName=***)
<-- UserService.provisionUser returned CurrentUserView[id=1, …, email=***] in 20ms
```

> **Значення чутливих полів маскуються завжди**, навіть на DEBUG: email, телефон, ім'я/прізвище,
> `value` контакту, паролі/токени/підписи (перелік — `LoggingConstants.SENSITIVE_KEYS`).
> Рівні цих логерів задані явно, тому `APP_LOG_LEVEL` на них не впливає — тільки змінні вище.
> Тіло запиту буферизується не більше 8 КБ, довгі payload'и обрізаються.

**Бізнес-події.** Сервіси логують `INFO` після кожної успішної зміни стану (реєстрація,
онбординг, магазин, товари, замовлення) з id сутностей — читання не логуються. Разом із
`requestId` це дає повну історію сценарію:

```
[corr-onb] u.c.b.b.s.a.SellerRegistrationService : Seller registered userId=1 accountId=1 onboardingStatus=NOT_STARTED
[corr-onb] u.c.b.bravi.seller.stores.StoreService : Draft store created storeId=1 publicId=st_xkwO… sellerAccountId=1
[corr-onb] u.c.b.b.seller.SellerOnboardingService : Onboarding completed accountId=1 storeId=1
```

> У логи не потрапляють PII: у MDC кладемо лише непрямі ідентифікатори. `InvocationContext`
> містить email, username та ім'я — логувати його (у т.ч. через `toString()`) не можна.

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
