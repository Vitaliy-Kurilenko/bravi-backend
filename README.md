# Bravi

Backend-застосунок на **Spring Boot 4.x / Java 26**, PostgreSQL, інтеграція з Keycloak.
Деталі конвенцій розробки — у [CLAUDE.md](CLAUDE.md).

## Функціонал

- **REST API** на HTTP-шарі (`controller/`), формат помилок — RFC 9457 `ProblemDetail`.
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

Усі під `/api` (context-path), потребують валідного JWT і заголовка `X-Correlation-Id`. Поточний користувач визначається з токена (через `InvocationContext`), без id у шляху.

| Метод | Шлях | Опис | Відповідь |
|-------|------|------|-----------|
| GET | `/users/context` | Контекст поточного користувача | `200` `UserResponse` |
| GET | `/stores` | Магазин поточного користувача | `200` `StoreResponse`; `404` якщо нема |
| POST | `/stores` | Створити магазин (лише `SELLER`) | `201`; `409` якщо вже є; `403` не-SELLER; `400` валідація |
| PATCH | `/stores` | Часткове оновлення магазину (лише `SELLER`) | `204`; `404`/`403`/`400` |

Магазин прив'язаний до користувача один-до-одного (`stores.seller_id` UNIQUE). `status` магазину — серверно-кероване (дефолт `ACTIVE`), з клієнта не приймається; на PATCH оновлюються тільки передані поля.

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
