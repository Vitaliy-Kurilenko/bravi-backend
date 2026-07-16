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
- **`seller`** — вертикаль продавця: реєстрація seller-акаунта, онбординг, магазини,
  товари/категорії/виробники магазину, замовлення продавця. REST під префіксами `/seller/**`
  (day-to-day) та `/accounts/{accountId}/seller/onboarding/**` (онбординг).

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
