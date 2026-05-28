# Bravi — правила та обмеження

Spring Boot 4.x застосунок на Java 26, PostgreSQL, інтеграція з Keycloak.

## 1. Структура проекту

Базовий пакет: `ua.com.bravi.bravi`. Шари суворо розділені — кросові імпорти заборонені (наприклад, `controller` не імпортує `persistance` напряму).

```
ua.com.bravi.bravi
├── client/                  REST-клієнти до зовнішніх систем (Keycloak тощо)
│   └── dto/                 DTO для запитів/відповідей зовнішніх API
├── common/                  Спільні константи проекту (HttpConstants тощо)
├── component/               Допоміжні Spring-компоненти (фільтри, парсери,
│                            request-scoped біни — не сервіси й не домен)
├── config/                  Spring-конфігурації (WebConfig, SecurityConfig, ...)
│   ├── props/               @ConfigurationProperties класи
│   └── restclient/          Конфігурація RestClient бінів
├── controller/              REST-контролери (HTTP-шар)
│   └── dto/
│       ├── in/              Request-DTO (вхідні)
│       └── out/             Response-DTO (вихідні)
├── domain/                  Бізнес-логіка та бізнес-правила
│   └── <aggregate>/         Згруповано по агрегатах (наприклад, domain/user)
│       ├── <Aggregate>.java       Доменна модель (POJO, без JPA-анотацій)
│       ├── <Aggregate>Policy.java Правила/інваріанти
│       └── <Aggregate>Service.java Use-case'и над агрегатом (опційно)
├── exception/               Кастомні exception'и
│   └── dto/                 DTO для error response (FiledValidationError тощо)
├── persistance/             JPA-шар: репозиторії та entity
│   ├── entity/              JPA @Entity класи
│   └── enums/               Enum'и, що зберігаються в БД
└── service/                 Application-сервіси — оркестрація use-case'ів
                             (комбінують domain + persistance + client)
```

**Правила залежностей між шарами:**

- `controller` → `service` → `domain` / `persistance` / `client`
- `domain` НЕ залежить від `persistance`, `controller`, `client` — це чиста бізнес-логіка
- `persistance.entity` ніколи не повертається з `controller` — мапиться у `controller.dto.out` через MapStruct
- `controller.dto.in` ніколи не зберігається напряму — мапиться у domain-модель або entity

## 2. Lombok

Lombok обов'язковий для скорочення boilerplate. Використовуй:

- `@Getter` / `@Setter` — замість ручних геттерів/сеттерів
- `@RequiredArgsConstructor` — для DI у сервіси, контролери, компоненти (поля `private final`)
- `@Builder` — для DTO, доменних моделей, складних об'єктів
- `@Value` — для незмінних DTO/value-object'ів
- `@Slf4j` — замість ручного оголошення логера
- `@EqualsAndHashCode` / `@ToString` — обережно, з `onlyExplicitlyIncluded` для JPA-entity (щоб уникнути проблем з лінивими колекціями)

**Не використовуй** `@Data` на JPA-entity — він генерує `equals`/`hashCode`/`toString` по всіх полях, що ламає lazy loading і викликає `StackOverflowError` на bidirectional-зв'язках.

DI робимо через constructor injection (`@RequiredArgsConstructor`), а не `@Autowired` на полях.

## 3. Flyway міграції

Усі зміни схеми БД — тільки через Flyway. Ручні зміни через консоль БД заборонені.

- Локація: `src/main/resources/db.migration/`
- Іменування: `V<number>__<snake_case_description>.sql` (приклад: `V3__table_users_add_email.sql`)
- Номери монотонно зростаючі — не змінюй уже застосовані міграції; додавай нову
- Одна міграція — одна логічна зміна (створення таблиці, додавання колонки, бекфіл даних)
- DDL і важкий DML розділяй на окремі міграції, щоб транзакції залишалися короткими
- Repeatable-міграції (`R__*.sql`) використовуй лише для view/функцій/процедур

## 4. MapStruct

Усі мапери між шарами (entity ↔ domain ↔ DTO) — через MapStruct. Ручне поле-за-полем копіювання заборонене.

- Інтерфейс мапера: `@Mapper(componentModel = "spring")` — інжектиться як Spring-бін
- Розташування: поряд із цільовим шаром (наприклад, `controller/mapper/UserDtoMapper.java`, `persistance/mapper/UserEntityMapper.java`)
- Іменування методів: `toDto`, `toDomain`, `toEntity`, `updateEntity` (для in-place оновлення з `@MappingTarget`)
- Складні перетворення — через `@Mapping(target = "...", expression = "...")` або вкладений мапер у `uses = { ... }`
- Уникай використання `@AfterMapping` для бізнес-логіки — це місце для тех-деталей мапінгу, не правил

## 5. Бізнес-логіка → шар domain

Бізнес-правила, інваріанти агрегатів, доменні рішення живуть у `domain/`. `service/` лише оркеструє: завантажує дані з `persistance`, передає в `domain`, зберігає результат, викликає `client` за потреби.

**Що йде в `domain`:**

- Доменні моделі (POJO, без JPA, без Spring)
- Перевірки інваріантів (наприклад, `User.canBeActivated()`, `OrderPolicy.applyDiscount()`)
- Перетворення стану з бізнес-сенсом (`order.cancel()` замість `order.setStatus(CANCELLED)` у сервісі)

**Що НЕ йде в `domain`:**

- Виклики репозиторіїв чи зовнішніх клієнтів (це робота `service`)
- HTTP/REST логіка
- JPA-анотації, Spring-анотації (`@Service`, `@Component`)

Сервіс повинен читатися як сценарій use-case'у, а не містити `if`-и з бізнес-правилами.

## 6. Винятки

- Кастомні exception'и — у `exception/`, успадковані від `RuntimeException`
- Глобальний обробник — `GlobalExceptionHandler` (`@RestControllerAdvice`)
- Validation-помилки повертай у форматі `FiledValidationError` (поле + повідомлення)
- Зовнішні виклики (Keycloak тощо) загортай у `ExternalServiceException` / `KeycloakClientException`

## 7. Тести

- Інтеграційні тести проти реальної БД (Testcontainers) — не мокай JPA-репозиторії в integration-шарі
- Доменну логіку покривай unit-тестами без Spring-контексту (швидко, ізольовано)
- Сервіси — `@SpringBootTest` або slice-тести (`@DataJpaTest`, `@WebMvcTest`) залежно від шару

## 8. Конфігурація

- `application.yaml` — основна конфігурація
- Кастомні properties — через `@ConfigurationProperties` у `config/props/` (а не `@Value`)
- **Значення, що не змінюються між середовищами** (фіксовані набори заголовків, URL-патерни роутів, технічні переліки) — це `public static final` константи у `common/`, НЕ в `application.yaml`. Конфіг — лише для того, що реально варіюється
- Секрети не комітимо — через env-змінні чи зовнішні vault'и

## 9. Cross-cutting infrastructure (HTTP)

Кожен бізнес-запит проходить ланцюжок фільтрів:

```
order  filter                          відповідальність
─────  ────────────────────────────── ─────────────────────────────────────
 -200  RequiredHeadersFilter          валідує наявність кожного заголовка
                                      зі списку HttpConstants.REQUIRED_HEADERS;
                                      400 ProblemDetail при відсутності
 -190  RequestIdMdcFilter             MDC.put("requestId") + echo у response;
                                      ставиться ДО security, щоб логи 401/403
                                      теж корелювались
 -100  Spring Security FilterChainProxy  валідація JWT через JWKS Keycloak;
                                      401 при невалідному/відсутньому токені
    0  InvocationContextFilter        читає SecurityContext + headers,
                                      заповнює @RequestScope InvocationContext
```

- **Обов'язкові заголовки і виключені шляхи** — список у `common/HttpConstants.java` (`REQUIRED_HEADERS`, `EXCLUDED_PATHS`). Додати новий обов'язковий заголовок — один рядок у константі
- **JWT** — Spring Security resource server, `issuer-uri` зібрано з `keycloack.base-url` + `keycloack.realm`; ролі витягуються з claim'а `realm_access.roles` Keycloak
- **`InvocationContext`** — `@RequestScope` бін у `component/`. Поля: `requestId`, `userExtId`, `username`, `email`, `roles`, `device`. Інжектиться через constructor injection куди потрібно
- **401/403** — повертаються кастомними `ProblemDetail*EntryPoint`/`*AccessDeniedHandler` напряму у response (Spring Security кидає до MVC-диспатчера, `@RestControllerAdvice` там не активний)
- **Фільтри реєструються через `FilterRegistrationBean`** у `config/WebConfig.java` — самі класи фільтрів НЕ позначені `@Component`, щоб уникнути подвійної автореєстрації

## 10. README

Перед кожним комітом обов'язково оновлюй `README.md`, якщо зміни торкаються функціоналу, переліку чи опису змінних оточення, способу запуску або конфігурації. `README.md` має лишатися актуальним джерелом правди для запуску й налаштування застосунку.
