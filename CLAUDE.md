# Bravi — правила та обмеження

Spring Boot 4.x застосунок на Java 26, PostgreSQL, інтеграція з Keycloak.
Архітектура — **модульний моноліт на Spring Modulith 2.x**: код поділено на бізнес-модулі,
межі між якими enforced'яться Spring Modulith (`@ApplicationModule`, `@NamedInterface`).

## 1. Структура проекту — модулі Spring Modulith

Базовий пакет: `ua.com.bravi.bravi`. Кожен пакет першого рівня під ним — це **модуль**
(`@ApplicationModule` у `package-info.java`). Модулі спілкуються між собою **тільки через
опубліковані named interface'и** (`api`); прямий імпорт внутрішніх класів чужого модуля заборонений
і ламає `ModulithStructureTest` (`ApplicationModules.verify()`).

```
ua.com.bravi.bravi
├── shared/        @ApplicationModule(type = OPEN) — крос-катінг інфраструктура,
│                  доступна всім модулям без обмежень
│   ├── common/        public static final константи (HttpConstants тощо)
│   ├── component/     фільтри, request-scope біни (InvocationContext),
│   │                  ProblemDetail-handler'и, парсери, маркер-анотації
│   ├── config/        глобальні @Configuration (WebConfig, SecurityConfig)
│   ├── exception/     базові кросові exception'и + GlobalExceptionHandler (+ dto/)
│   └── util/          stateless-хелпери (ValidationPatterns, конвертери)
│
├── identity/      @ApplicationModule — агрегат User (хто такий користувач)
│   ├── api/           @NamedInterface("api"): IdentityApi, CurrentUserView,
│   │                  event/UserProvisionedEvent
│   ├── UserService.java   корінь: implements IdentityApi (JIT-provisioning з JWT)
│   ├── controller/    UserController (/users/context) + dto/out/ + mapper/
│   ├── component/     CurrentUserInterceptor · config/ IdentityWebConfig
│   ├── domain/        User, UserStatus (без типу — роль визначає membership в access)
│   └── persistence/   UserEntity, IUserEntityRepository, mapper/
│
├── access/        @ApplicationModule — tenancy + RBAC (як перевіряємо доступ)
│   ├── api/           @NamedInterface("api"): AccessApi, AccessContextView, AccountView,
│   │                  CurrentAccountHolder (@RequestScope місток до поточного акаунта)
│   ├── AccessService.java  implements AccessApi: resolveCurrentContext,
│   │                  provisionOwnerAccount (створює account + owner-membership)
│   ├── security/      AccessPermissionEvaluator (Spring Security PermissionEvaluator bean)
│   ├── domain/        Account, AccountType, AccountStatus, MembershipStatus
│   └── persistence/   accounts, memberships (+ roles/permissions/join-таблиці;
│                      role/permission codes читаються native-запитами)
│
└── seller/        @ApplicationModule — вертикаль продавця (власний домен + REST /seller/**)
    ├── AccountService.java         агрегує /accounts (identity + access + seller.account)
    ├── SellerOnboardingService.java оркеструє онбординг (§5): DRAFT-store + settings + manual-channel,
    │                  replace-contacts, complete → account ACTIVE / onboarding COMPLETED / store ACTIVE
    ├── controller/    seller-facing контролери (per-method @PreAuthorize hasPermission)
    │                  + InternalSellerRegistrationController (POST /internal/registrations/seller)
    │                  + AccountController (GET /accounts, authenticated)
    │                  + SellerOnboardingController (/accounts/{accountId}/seller/onboarding/**)
    │                  (+ dto/in/, dto/out/, mapper/)
    ├── exception/     онбординг-винятки (EmailNotVerified, OnboardingIncomplete, StoreAlreadyExists)
    │                  + SellerOnboardingExceptionHandler
    ├── account/       вкладений модуль: seller_accounts; SellerRegistrationApi (реєстрація від
    │                  Auth Service: User+Account+SellerAccount+Membership) + SellerAccountsApi
    ├── stores/        вкладений модуль: Store (DRAFT/ACTIVE/DISABLED/ARCHIVED), StoreSettings,
    │                  contacts/ (StoreContact); StoresApi, StoreView, StoreDraft, CurrentStoreHolder
    ├── channels/      вкладений модуль: sales_channels (MANUAL); SalesChannelsApi.createManualChannel
    ├── catalog/       вкладені модулі: categories/, manufacturers/, products/ (store-scoped)
    └── orders/        вкладений модуль: замовлення продавця (OrdersApi, order_statuses довідник)
```

> Створення магазину — **тільки через онбординг** (`SellerOnboardingController`); `/seller/stores`
> лишає GET/PATCH для day-to-day. Онбординг обмежений одним магазином на seller-акаунт
> (`unique(stores.seller_account_id)`).

> `buyer/` — порожня заготовка presentation-модуля, слейтед на видалення.
> Платформенний `catalog` (глобальні категорії/виробники) та `supplier` — заплановані, ще не реалізовані.

**Типи модулів:**

- **`shared`** — `@ApplicationModule(type = OPEN)`. Інфраструктура без власної бізнес-логіки;
  з нього можна імпортувати будь-де. Сюди НЕ кладемо бізнес-правила чи доменні моделі.
- **Бізнес-модулі** (`identity`, `access`, `seller` та його вкладені) — володіють своїми даними
  (entity, таблиці) та доменом. Назовні віддають лише `api`.
- **`seller`** — вертикаль: має власний домен/persistence (магазини, товари, замовлення) І
  REST-контролери під роль SELLER; вкладені під-модулі — окремі агрегати з власними `api`.
- **`access`** — крос-катінг для авторизації: інші модулі беруть поточний акаунт/права через
  `access.api` (`CurrentAccountHolder`, `AccessApi`) + `AccessPermissionEvaluator`.

**Правила залежностей:**

- Модуль звертається до іншого модуля **тільки через його `api`** (`<Other>Api` + `*View` + events).
  Імпорт `*.persistence.*`, `*.domain.*` (якщо не позначений як named interface), `*Service`
  чужого модуля — заборонений.
- `seller` бере поточний акаунт/права та ідентичність через `access.api` / `identity.api`;
  усередині `seller` контролери оркеструють вкладені `<sub>Api`/`<sub>Service`.
- Усередині модуля: `controller` → `<Module>Service` → `domain` / `persistence`.
- `domain` НЕ залежить від `persistence`, `controller`, presentation — це чиста бізнес-логіка.
- `persistence.entity` ніколи не «витікає» з модуля — назовні віддаємо `*View`-record (api)
  або DTO (controller), маплячи через MapStruct.
- `controller.dto.in` ніколи не зберігається напряму — мапиться у domain-модель.
- Перевіряй межі тестом Spring Modulith (`ApplicationModules.verify()`), а не на око.

## 2. Взаємодія модулів: API та події

- **Синхронний виклик** — через named interface `api`: бізнес-модуль публікує інтерфейс
  `<Module>Api` (його `implements` кореневий `<Module>Service`) і read-model record'и `*View`.
  Інший модуль інжектить саме інтерфейс, не реалізацію.
- **Асинхронна інтеграція / зменшення зв'язності** — через **доменні події**:
  публікуємо `ApplicationEventPublisher.publishEvent(new XxxEvent(...))`, event-record живе
  у `<module>/api/event/`. Слухачі (в інших модулях) — `@ApplicationModuleListener`.
- Події гарантовано доставляються через **Spring Modulith Event Publication Registry**
  (`spring-modulith-events-jpa`) — публікації персистяться в таблицю `event_publication`.
  **Схему цієї таблиці тримай у Flyway-міграції актуальною під версію Spring Modulith**
  (у 2.x таблиця має колонки `status`, `completion_attempts`, `last_resubmission_date`);
  розбіжність ловиться на старті як `SchemaManagementException` (бо `ddl-auto: validate`).
- Request-scope «місток» між шарами (наприклад `CurrentAccountHolder` в access,
  `CurrentStoreHolder` в seller.stores) тримай в `api/`, щоб інші модулі могли ним
  користуватись, а інжекцію `<Module>Api` роби `@Lazy`, якщо це request-scope бін.

## 3. Lombok

Lombok обов'язковий для скорочення boilerplate. Використовуй:

- `@Getter` / `@Setter` — замість ручних геттерів/сеттерів
- `@RequiredArgsConstructor` — для DI у сервіси, контролери, компоненти (поля `private final`)
- `@Builder` — для DTO, доменних моделей, складних об'єктів
- `@Value` — для незмінних DTO/value-object'ів (де не використано `record`)
- `@Slf4j` — замість ручного оголошення логера
- `@EqualsAndHashCode` / `@ToString` — обережно, з `onlyExplicitlyIncluded` для JPA-entity

**Не використовуй** `@Data` на JPA-entity — він генерує `equals`/`hashCode`/`toString` по всіх
полях, що ламає lazy loading і викликає `StackOverflowError` на bidirectional-зв'язках.

DI — через constructor injection (`@RequiredArgsConstructor`), а не `@Autowired` на полях.
Для незмінних публічних контрактів (`*View`, `*Event`, прості DTO) надавай перевагу `record`.

## 4. Flyway міграції

Усі зміни схеми БД — тільки через Flyway. Ручні зміни через консоль БД заборонені.

- Локація: `src/main/resources/db.migration/` (саме з крапкою; задано
  `spring.flyway.locations: classpath:db.migration` в `application.yaml`)
- Іменування: `V<number>__<snake_case_description>.sql` (приклад: `V3__table_store_contacts_create.sql`)
- Номери монотонно зростаючі — **не змінюй уже застосовані міграції; додавай нову**
- Одна міграція — одна логічна зміна (створення таблиці, додавання колонки, бекфіл даних)
- DDL і важкий DML розділяй на окремі міграції, щоб транзакції залишалися короткими
- Repeatable-міграції (`R__*.sql`) — лише для view/функцій/процедур
- Інфраструктурні таблиці (напр. `event_publication` Spring Modulith) теж версіонуються Flyway;
  при апгрейді Spring Modulith додавай міграцію під нову схему реєстру подій

## 5. MapStruct

Усі мапери між шарами (entity ↔ domain ↔ View/DTO) — через MapStruct. Ручне поле-за-полем заборонене.

- Інтерфейс мапера: `@Mapper(componentModel = "spring")` — інжектиться як Spring-бін
- Розташування — поряд із цільовим шаром усередині модуля:
  `persistence/mapper/*EntityMapper.java` (entity ↔ domain / view),
  `controller/mapper/*DtoMapper.java` (DTO ↔ domain)
- Іменування методів: `toDto` / `toResponse`, `toDomain`, `toEntity`, `toView`,
  `updateEntity` (in-place оновлення з `@MappingTarget`)
- Складні перетворення — `@Mapping(target = "...", expression = "...")` або вкладений мапер у `uses = { ... }`
- `@AfterMapping` — лише для тех-деталей мапінгу, не для бізнес-логіки

## 6. Бізнес-логіка → шар domain

Бізнес-правила, інваріанти агрегатів, доменні рішення живуть у `<module>/domain/`.
Кореневий `<Module>Service` лише оркеструє: завантажує дані з `persistence`, передає в `domain`,
зберігає результат, публікує події, за потреби кличе `api` іншого модуля.

**Що йде в `domain`:**

- Доменні моделі (POJO/`record`, без JPA, без Spring)
- Перевірки інваріантів (наприклад, `User.canBeActivated()`, `StoreContactPolicy`)
- Перетворення стану з бізнес-сенсом (`order.cancel()` замість `order.setStatus(CANCELLED)` у сервісі)

**Що НЕ йде в `domain`:**

- Виклики репозиторіїв, `api` інших модулів, публікація подій (це робота `<Module>Service`)
- HTTP/REST логіка
- JPA-анотації, Spring-анотації (`@Service`, `@Component`)

Сервіс повинен читатися як сценарій use-case'у, а не містити `if`-и з бізнес-правилами.

## 7. Винятки

- Кастомні exception'и — у `<module>/exception/` (або `shared/exception/` для кросових),
  успадковані від `RuntimeException`
- **Кросовий fallback** — `shared/exception/GlobalExceptionHandler` (`@RestControllerAdvice`)
- **Per-module handler'и** — `<Module>ExceptionHandler` (`@RestControllerAdvice`) для специфічних
  exception'ів модуля (`SellerAccountExceptionHandler`, `StoreContactsExceptionHandler`, `ProductsExceptionHandler`)
- **Порядок advice'ів обов'язково задавай через `@Order`** — глобальний `GlobalExceptionHandler`
  з catch-all `@ExceptionHandler(Exception.class)` має `@Order(Ordered.LOWEST_PRECEDENCE)` (останній,
  fallback), а кожен модульний handler — вищий пріоритет (`@Order(Ordered.LOWEST_PRECEDENCE - 100)`).
  Spring обирає handler перебором advice-бінів за `@Order` (а НЕ за специфічністю винятку між різними
  advice), тож без явного порядку catch-all перехоплює доменні винятки як 500 ще до модульного handler'а
- Validation-помилки повертай у форматі `FiledValidationError` (поле + повідомлення)
- Зовнішні виклики (Keycloak тощо) загортай у `ExternalServiceException`

## 8. Тести

- Перевірка модульних меж — Spring Modulith `ApplicationModules.of(...).verify()` (обов'язково)
- Інтеграційні тести проти реальної БД (Testcontainers) — не мокай JPA-репозиторії в integration-шарі
- Доменну логіку покривай unit-тестами без Spring-контексту (швидко, ізольовано)
- Сервіси/контролери — `@SpringBootTest` або slice-тести (`@DataJpaTest`, `@WebMvcTest`)
- Тести на взаємодію модулів через події — `Scenario` API зі `spring-modulith-starter-test`

## 9. Конфігурація

- `application.yaml` — основна конфігурація
- Кастомні properties — через `@ConfigurationProperties` у `config/props/` (а не `@Value`)
- **Значення, що не змінюються між середовищами** (фіксовані набори заголовків, URL-патерни,
  технічні переліки) — це `public static final` константи у `shared/common/`, НЕ в `application.yaml`.
  Конфіг — лише для того, що реально варіюється між середовищами
- Секрети не комітимо — через env-змінні чи зовнішні vault'и

## 10. Cross-cutting infrastructure (HTTP)

Глобальні фільтри живуть у `shared/` і реєструються у `shared/config/WebConfig.java`.
Кожен бізнес-запит проходить ланцюжок:

```
order  filter                          відповідальність
─────  ────────────────────────────── ─────────────────────────────────────
 -200  RequiredHeadersFilter          валідує наявність кожного заголовка
                                      зі списку HttpConstants.REQUIRED_HEADERS;
                                      400 ProblemDetail при відсутності
 -190  RequestIdMdcFilter             MDC.put("requestId") + echo у response;
                                      ДО security, щоб логи 401/403 теж корелювались
 -100  Spring Security FilterChainProxy  валідація JWT через JWKS Keycloak; 401 при невалідному
    0  InvocationContextFilter        читає SecurityContext + headers,
                                      заповнює @RequestScope InvocationContext
```

- **Обов'язкові заголовки і виключені шляхи** — `shared/common/HttpConstants.java`
  (`REQUIRED_HEADERS`, `EXCLUDED_PATHS`). Додати новий — один рядок у константі
- **JWT** — Spring Security resource server, `issuer-uri` зібрано з `keycloack.base-url` + `keycloack.realm`;
  ролі — з claim'а `realm_access.roles` Keycloak
- **`InvocationContext`** — `@RequestScope` бін у `shared/component/`. Інжектиться через
  constructor injection куди потрібно (зокрема в `api`-холдери інших модулів)
- **401/403** — кастомні `ProblemDetail*EntryPoint`/`*AccessDeniedHandler` пишуть напряму в response
  (Spring Security кидає до MVC-диспатчера, `@RestControllerAdvice` там не активний)
- **Глобальні фільтри — через `FilterRegistrationBean`** у `shared/config/WebConfig.java`;
  самі класи фільтрів НЕ позначені `@Component`, щоб уникнути подвійної автореєстрації
- **Module-local MVC-розширення** — у `<module>/config/*WebConfig` (`WebMvcConfigurer`):
  напр. `StoresWebConfig`/`IdentityWebConfig` реєструють свої interceptor'и з
  `excludePathPatterns(HttpConstants.EXCLUDED_PATHS)`
- **Маркер-анотації доступу до стану** (`shared/component`): `@RequireStore` (на класі/методі)
  вимагає наявності магазину в `CurrentStoreHolder`, `@PermitNoStore` — точкове виключення
  (напр. створення першого магазину). Перевірку робить `StoreRequiredInterceptor`

## 10a. Авторизація (RBAC)

Тришарова: (1) service-to-service гейт — `/internal/**` → `hasAuthority(<internal-role>)` (за
замовч. `service_registration`, конфігуровано `bravi.security.internal-role`); токен видає Keycloak
service-account Auth-сервісу; (2) грубий HTTP-гейт `/seller/**` та `/accounts/**` (онбординг) →
`hasAuthority('role_seller')` (Keycloak realm-роль); (3) тонка per-method перевірка прав —
`@PreAuthorize("hasPermission('RESOURCE','ACTION')")` на кожному ендпоінті контролера (READ на GET,
WRITE на мутаціях).

- `hasPermission(...)` маршрутизується в `access.security.AccessPermissionEvaluator` через
  `MethodSecurityExpressionHandler`-бін (`SecurityConfig`); evaluator читає набір прав поточного
  акаунта з `CurrentAccountHolder` (коди виду `STORE_WRITE`, `PRODUCT_READ`, `ORDER_WRITE`)
- Коди прав і system-ролі (`SELLER_OWNER`/`SELLER_MEMBER`) засіяно Flyway-міграцією; ресурси
  categories/manufacturers мапляться на `PRODUCT_*`, контакти магазину — на `STORE_*`
- `/internal/**` виключено з `CurrentUserInterceptor` (`HttpConstants.NON_USER_PATHS`) — сервісний
  токен не резолвиться в кінцевого користувача; реєстрація бере `keycloakUserId` з тіла запиту
- Провіженінг користувача — **тільки явний** (`IdentityApi.provisionUser` через реєстрацію);
  `resolveCurrentUser` — lookup-only, не створює. JIT-провіженінгу немає
- Онбординг (`/accounts/{accountId}/seller/onboarding/**`): `SellerOnboardingService` звіряє path
  `{accountId}` (public id) з поточним акаунтом (`resolveCurrentContext`) і кидає 403 при розбіжності
  чи не-SELLER акаунті — на додачу до per-method `hasPermission('STORE', ...)`
- Новий ресурс → додай пару `*_READ/*_WRITE` у seed-міграцію і `@PreAuthorize` на методи

## 11. README

Перед кожним комітом обов'язково оновлюй `README.md`, якщо зміни торкаються функціоналу,
переліку чи опису змінних оточення, способу запуску або конфігурації. `README.md` має лишатися
актуальним джерелом правди для запуску й налаштування застосунку.
