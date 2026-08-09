# GateKeeper — Technical Design Document

## 1. Архитектура

### Тип: Модульный монолит (single Spring Boot application)

**WHY монолит, а не микросервисы:**
- Auth Server и API Gateway — тесно связанные компоненты одного продукта. Gateway валидирует токены, которые выдает Auth Server. Разделение на 2 процесса добавляет network hop и точку отказа без реального выигрыша.
- Spring Authorization Server (Servlet) и кастомный gateway proxy (тоже Servlet) работают в одном web-стеке. Конфликтов нет.
- Для portfolio-проекта единое приложение проще запускать, тестировать и демонстрировать.
- В production при необходимости gateway выносится в отдельный сервис — архитектура это позволяет (домены изолированы по пакетам).

**WHY кастомный proxy, а не Spring Cloud Gateway:**
- Spring Cloud Gateway работает на WebFlux (reactive). Spring Authorization Server — на Servlet. Они несовместимы в одном процессе.
- Spring Cloud Gateway MVC (servlet-вариант) появился в Spring Cloud 2023.0, но имеет ограниченную документацию и неочевидную совместимость с Authorization Server.
- Кастомный proxy на RestClient + Filter Chain дает полный контроль, прозрачность и демонстрирует паттерн Chain of Responsibility.
- Gateway функциональность — это ~15% кодовой базы: ProxyController, RouteResolver, 5 фильтров. Это не "написать свой веб-фреймворк", а конкретная задача с ограниченным scope.

### Диаграмма компонентов

```
┌─────────────────────────────────────────────────────┐
│                  GateKeeper App (:8080)               │
│                                                       │
│  ┌──────────────┐  ┌──────────────┐  ┌─────────────┐ │
│  │  Auth Server  │  │  Admin API   │  │   Gateway    │ │
│  │  (OAuth2/OIDC)│  │  (REST CRUD) │  │   (Proxy)    │ │
│  │              │  │              │  │              │ │
│  │ /oauth2/*    │  │ /api/v1/*    │  │ /** catch-all│ │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘ │
│         │                 │                 │         │
│  ┌──────┴─────────────────┴─────────────────┴───────┐ │
│  │              Spring Security Filter Chain          │ │
│  └──────┬─────────────────┬─────────────────────────┘ │
│         │                 │                           │
│  ┌──────┴───────┐  ┌──────┴───────┐                   │
│  │  PostgreSQL   │  │    Redis     │                   │
│  │  (JPA)        │  │  (Bucket4j)  │                   │
│  └──────────────┘  └──────────────┘                   │
└─────────────────────────────────────────────────────┘
```

### Поток запроса через Gateway

```
HTTP Request
  │
  ▼
Spring Security Filter Chain (JWT validation для /api/v1/**)
  │
  ▼
DispatcherServlet
  │
  ├── /oauth2/**        → Spring Authorization Server (автоматически)
  ├── /api/v1/**        → Admin REST Controllers (@RestController)
  └── /** (остальное)   → ProxyController (@Order(LOWEST_PRECEDENCE))
                              │
                              ▼
                        RouteResolver.resolve(path)
                              │
                         ┌────┴────┐
                         │ Route   │ → 404 если не найден
                         │ found?  │
                         └────┬────┘
                              │ yes
                              ▼
                        GatewayFilterChain
                         1. IpFilter
                         2. RateLimitFilter
                         3. AuthFilter
                         4. TransformationFilter
                              │
                              ▼
                        ProxyService.forward(request, route)
                         (RestClient → upstream)
                              │
                              ▼
                        TrafficLoggingFilter (post-processing)
                              │
                              ▼
                        HTTP Response
```

---

## 2. Структура пакетов

```
com.ashevtsov.gatekeeper/
├── GatekeeperApplication.java
│
├── config/                          — конфигурация Spring
│   ├── AuthorizationServerConfig    — Spring Authorization Server: ключи, token settings
│   ├── SecurityConfig               — SecurityFilterChain бины (3 цепочки)
│   ├── RedisConfig                  — RedisTemplate, connection factory
│   ├── CacheConfig                  — Caffeine cache manager
│   ├── RestClientConfig             — RestClient bean для proxy
│   └── OpenApiConfig                — SpringDoc группировка endpoints
│
├── security/                        — сквозная security-логика
│   ├── CustomUserDetailsService     — загрузка User + roles + permissions для auth
│   ├── JwtTokenCustomizer           — добавление tenantId, roles в JWT claims
│   └── TenantContext                — ThreadLocal<UUID> текущего тенанта
│
├── user/                            — домен "Пользователи"
│   ├── User                         — @Entity
│   ├── UserRepository               — JpaRepository + custom queries
│   ├── UserService                  — бизнес-логика
│   ├── UserController               — @RestController /api/v1/users
│   ├── UserMapper                   — @Mapper (MapStruct)
│   └── dto/
│       ├── CreateUserRequest        — @Valid request
│       ├── UpdateUserRequest
│       └── UserResponse
│
├── role/                            — домен "Роли и разрешения"
│   ├── Role                         — @Entity (self-ref parent)
│   ├── Permission                   — @Entity
│   ├── RoleRepository
│   ├── PermissionRepository
│   ├── RoleService
│   ├── RoleController               — /api/v1/roles
│   ├── RoleMapper
│   └── dto/
│
├── client/                          — домен "OAuth2 клиенты"
│   ├── OAuthClientService           — обертка над RegisteredClientRepository
│   ├── OAuthClientController        — /api/v1/clients
│   ├── OAuthClientMapper
│   └── dto/
│
├── tenant/                          — домен "Тенанты"
│   ├── Tenant                       — @Entity
│   ├── TenantRepository
│   ├── TenantService
│   ├── TenantController             — /api/v1/tenants
│   ├── TenantMapper
│   └── dto/
│
├── session/                         — домен "Сессии"
│   ├── UserSession                  — @Entity
│   ├── UserSessionRepository
│   ├── SessionService
│   └── SessionController            — /api/v1/sessions
│
├── gateway/                         — домен "API Gateway"
│   ├── GatewayRoute                 — @Entity маршрута
│   ├── RouteTransformation          — @Entity трансформации заголовков
│   ├── GatewayRouteRepository
│   ├── RouteService                 — CRUD маршрутов + cache eviction
│   ├── RouteController              — /api/v1/routes
│   ├── RouteMapper
│   ├── RouteResolver                — резолв маршрута по path (+ Caffeine cache)
│   ├── ProxyController              — @RequestMapping("/**") catch-all
│   ├── ProxyService                 — RestClient forward
│   ├── filter/
│   │   ├── GatewayFilter            — interface: filter(GatewayContext, GatewayFilterChain)
│   │   ├── GatewayFilterChain       — List<GatewayFilter>, index-based execution
│   │   ├── GatewayContext           — request, response, route, attributes map
│   │   ├── IpFilter                 — проверка IP whitelist/blacklist
│   │   ├── RateLimitFilter          — Bucket4j + Redis
│   │   ├── AuthFilter               — JWT scope проверка для route.requiredScopes
│   │   ├── TransformationFilter     — добавление/удаление заголовков
│   │   └── TrafficLoggingFilter     — async запись TrafficLog
│   └── dto/
│
├── ratelimit/                       — домен "Rate Limiting"
│   ├── RateLimitRule                — @Entity
│   ├── RateLimitRepository
│   ├── RateLimitService
│   ├── RateLimitController          — /api/v1/rate-limits
│   └── dto/
│
├── ipfilter/                        — домен "IP правила"
│   ├── IpRule                       — @Entity
│   ├── IpRuleRepository
│   ├── IpFilterService              — проверка IP + CIDR matching
│   ├── IpRuleController             — /api/v1/ip-rules
│   └── dto/
│
├── analytics/                       — домен "Аналитика"
│   ├── TrafficLog                   — @Entity (append-only)
│   ├── TrafficLogRepository         — native queries для агрегации
│   ├── AnalyticsService             — подсчет RPS, percentiles
│   ├── AnalyticsController          — /api/v1/analytics
│   └── dto/
│
├── audit/                           — сквозной аудит
│   ├── AuditLog                     — @Entity (append-only)
│   ├── AuditLogRepository
│   └── AuditService                 — @Async запись аудит-событий
│
└── common/                          — общие компоненты
    ├── dto/
    │   ├── PageResponse             — обертка для пагинации
    │   └── ErrorResponse            — стандартный формат ошибок
    └── exception/
        ├── GatekeeperException      — base exception
        ├── NotFoundException
        ├── ConflictException
        └── GlobalExceptionHandler   — @RestControllerAdvice
```

**WHY domain-driven packaging (по доменам, а не по слоям):**
- Код, относящийся к одному домену, находится рядом. Для понимания "как работают маршруты" — один пакет `gateway/`.
- Легко определить зависимости между доменами: если `gateway` импортирует из `tenant` — это явная зависимость.
- При выносе домена в отдельный сервис — копируем пакет целиком.
- По слоям (controller/, service/, repository/) приходится прыгать между папками для одной фичи.

---

## 3. Сущности

### User

```java
@Entity @Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = UUID) UUID id;
    @Column(unique = true, nullable = false, length = 50) String username;
    @Column(unique = true, nullable = false) String email;
    @Column(nullable = false) String passwordHash;       // BCrypt
    String firstName;
    String lastName;
    boolean enabled = true;

    @ManyToOne(fetch = LAZY) @JoinColumn(name = "tenant_id", nullable = false)
    Tenant tenant;

    @ManyToMany(fetch = LAZY)
    @JoinTable(name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id"))
    Set<Role> roles = new HashSet<>();

    Instant createdAt;
    Instant updatedAt;     // @PreUpdate
}
```

**WHY `Set<Role>` а не `List<Role>`:** роли уникальны для пользователя, Set предотвращает дупликаты на уровне Java. JPA корректно работает с Set для @ManyToMany.

### Role

```java
@Entity @Table(name = "roles", uniqueConstraints = @UniqueConstraint(columns = {"name", "tenant_id"}))
public class Role {
    @Id @GeneratedValue(strategy = UUID) UUID id;
    @Column(nullable = false, length = 100) String name;
    String description;

    @ManyToOne(fetch = LAZY) @JoinColumn(name = "parent_id")
    Role parent;                                          // иерархия

    @ManyToOne(fetch = LAZY) @JoinColumn(name = "tenant_id", nullable = false)
    Tenant tenant;

    @ManyToMany(fetch = LAZY)
    @JoinTable(name = "role_permissions",
        joinColumns = @JoinColumn(name = "role_id"),
        inverseJoinColumns = @JoinColumn(name = "permission_id"))
    Set<Permission> permissions = new HashSet<>();

    Instant createdAt;
}
```

**WHY self-referencing parent:** иерархия ролей (ADMIN → OPERATOR → VIEWER). При проверке прав — рекурсивно собираем permissions роли + всех parent-ов. Глубина иерархии ограничена (максимум 5 уровней, проверяется в сервисе).

### Permission

```java
@Entity @Table(name = "permissions", uniqueConstraints = @UniqueConstraint(columns = {"resource", "action"}))
public class Permission {
    @Id @GeneratedValue(strategy = UUID) UUID id;
    @Column(unique = true, nullable = false) String name;    // "users:read"
    @Column(nullable = false) String resource;               // "users"
    @Column(nullable = false) String action;                 // "READ"
    String description;
}
```

### Tenant

```java
@Entity @Table(name = "tenants")
public class Tenant {
    @Id @GeneratedValue(strategy = UUID) UUID id;
    @Column(nullable = false) String name;
    @Column(unique = true, nullable = false, length = 100) String slug;
    @Column(unique = true, nullable = false) String apiKey;  // gk_live_xxx
    boolean enabled = true;
    int maxRps = 100;
    Instant createdAt;
    Instant updatedAt;
}
```

**WHY apiKey в Tenant, а не отдельная таблица:** один тенант = один API-ключ. Нет сценария "несколько ключей на тенант". Если появится — вынесем в отдельную таблицу, но YAGNI.

### GatewayRoute

```java
@Entity @Table(name = "gateway_routes")
public class GatewayRoute {
    @Id @GeneratedValue(strategy = UUID) UUID id;

    @ManyToOne(fetch = LAZY) @JoinColumn(name = "tenant_id", nullable = false)
    Tenant tenant;

    @Column(nullable = false) String name;
    @Column(nullable = false) String predicatePath;     // "/users-service/**"
    @Column(nullable = false) String targetUrl;          // "http://user-service:8081"
    String methods;                                       // "GET,POST" или null = все
    int stripPrefix = 1;
    int orderPriority = 0;
    boolean enabled = true;
    boolean requireAuth = true;
    String requiredScopes;                                // "read,write" или null

    @OneToMany(mappedBy = "route", cascade = ALL, orphanRemoval = true)
    List<RouteTransformation> transformations = new ArrayList<>();

    Instant createdAt;
    Instant updatedAt;
}
```

**WHY `predicatePath` как String, а не regex:** Ant-style path matching (`/users/**`) проще, безопаснее и покрывает 99% use-case-ов. Regex добавляет ReDoS-risk и сложность. Используем `AntPathMatcher` из Spring.

### RouteTransformation, IpRule, RateLimitRule, UserSession, TrafficLog, AuditLog

Аналогичная структура: `@Entity` + JPA аннотации. Подробности в PRD секции "Сущности".

---

## 4. Database Schema

### Стратегия миграций: Liquibase (XML changesets)

**WHY Liquibase, а не Flyway:**
- Liquibase поддерживает preconditions (проверка перед выполнением), rollback, контекст-зависимые миграции.
- XML-формат гарантирует database-agnostic описание (хотя мы используем только PostgreSQL, это хорошая практика).
- В enterprise-проектах Liquibase встречается чаще (де-факто стандарт в крупных компаниях).

### Changelog порядок

```
db.changelog-master.xml
  ├── 001-create-tenants.xml
  ├── 002-create-users-roles-permissions.xml
  ├── 003-create-oauth2-tables.xml          — Spring Auth Server schema
  ├── 004-create-gateway-routes.xml
  ├── 005-create-ip-rules.xml
  ├── 006-create-rate-limit-rules.xml
  ├── 007-create-traffic-logs.xml
  ├── 008-create-audit-logs.xml
  ├── 009-create-user-sessions.xml
  └── 010-seed-data.xml                     — default tenant, SUPER_ADMIN, базовые permissions
```

### Индексы

```sql
-- часто фильтруем по tenant_id
create index idx_users_tenant on users(tenant_id);
create index idx_roles_tenant on roles(tenant_id);
create index idx_routes_tenant on gateway_routes(tenant_id);

-- поиск пользователей
create index idx_users_email on users(email);

-- логи: запросы по периодам
create index idx_traffic_tenant_time on traffic_logs(tenant_id, created_at);
create index idx_traffic_route_time on traffic_logs(route_id, created_at);
create index idx_audit_tenant_time on audit_logs(tenant_id, created_at);

-- сессии: поиск активных
create index idx_sessions_user on user_sessions(user_id);
create index idx_sessions_active on user_sessions(expires_at) where revoked = false;
```

**WHY partial index на sessions:** подавляющее большинство сессий — revoked. Partial index `where revoked = false` индексирует только активные, что на порядок меньше данных.

---

## 5. API Design

### Три SecurityFilterChain (по приоритету)

```java
// 1. OAuth2 Authorization Server endpoints
@Order(1) SecurityFilterChain authServerChain
    .securityMatcher("/oauth2/**", "/.well-known/**")
    // конфигурация Spring Authorization Server

// 2. Admin API endpoints
@Order(2) SecurityFilterChain adminApiChain
    .securityMatcher("/api/v1/**")
    .oauth2ResourceServer(jwt)
    // RBAC через @PreAuthorize

// 3. Gateway catch-all (все остальное)
@Order(3) SecurityFilterChain gatewayChain
    .securityMatcher("/**")
    .permitAll()  // auth проверяется в AuthFilter внутри GatewayFilterChain
```

**WHY 3 отдельные цепочки:** каждая зона (OAuth2, Admin API, Gateway) имеет разную security-семантику. OAuth2 endpoints управляются Spring Authorization Server. Admin API требует JWT + RBAC. Gateway — кастомная авторизация per route (некоторые маршруты публичные).

### Пагинация

Все list-эндпоинты используют Spring `Pageable`:
- `?page=0&size=20&sort=createdAt,desc`
- Response через `PageResponse<T>` wrapper (не Spring Page напрямую, чтобы не привязывать API к Spring).

### Версионирование API

Prefix `/api/v1/`. При breaking changes — `/api/v2/` рядом. **WHY URL-based, а не header-based:** URL-версионирование проще для Swagger UI, curl-примеров и документации.

---

## 6. Сервисы (Service Layer)

### Принцип: тонкий контроллер, толстый сервис

Контроллер: валидация (`@Valid`), маппинг DTO → domain, вызов сервиса, маппинг domain → response.
Сервис: бизнес-логика, транзакции (`@Transactional`), вызов репозитория, аудит.

### UserService — ключевые методы

```
createUser(CreateUserRequest) → UserResponse
    1. Проверить уникальность username и email (в рамках тенанта)
    2. Хешировать пароль (BCrypt)
    3. Назначить роли (по roleIds)
    4. Сохранить
    5. Записать аудит (CREATE_USER)

updateUser(UUID id, UpdateUserRequest) → UserResponse
    1. Найти пользователя (или 404)
    2. Проверить tenant scope (текущий тенант == тенант пользователя)
    3. Обновить поля
    4. Записать аудит (UPDATE_USER)
```

### RoleService — иерархия

```
getEffectivePermissions(Role role) → Set<Permission>
    1. Собрать permissions текущей роли
    2. Рекурсивно добавить permissions parent-роли
    3. Ограничение: maxDepth=5 (защита от циклов)
```

**WHY maxDepth=5:** защита от случайного цикла в parent-ссылках. 5 уровней иерархии покрывает любой реальный сценарий (SUPER_ADMIN → ADMIN → MANAGER → OPERATOR → VIEWER).

### OAuthClientService

Обертка над Spring `RegisteredClientRepository`:
```
registerClient(RegisterClientRequest) → ClientResponse
    1. Сгенерировать UUID для client secret
    2. Создать RegisteredClient через Spring API
    3. Сохранить через JdbcRegisteredClientRepository
    4. Вернуть secret в response (показывается один раз)
    5. Записать аудит
```

**WHY обертка, а не прямой доступ к RegisteredClientRepository:** Spring API оперирует объектом `RegisteredClient`, который не подходит как REST response. Нужен маппинг + аудит + tenant scope.

---

## 7. Repositories

### Стандартные

```java
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsernameAndTenantId(String username, UUID tenantId);
    Optional<User> findByEmailAndTenantId(String email, UUID tenantId);
    Page<User> findByTenantId(UUID tenantId, Pageable pageable);
    boolean existsByUsernameAndTenantId(String username, UUID tenantId);
}
```

### Специализированные

```java
public interface TrafficLogRepository extends JpaRepository<TrafficLog, Long> {
    // native query для агрегации (percentiles)
    @Query(nativeQuery = true, value = """
        select
            count(*) as total_requests,
            percentile_cont(0.50) within group (order by latency_ms) as p50,
            percentile_cont(0.95) within group (order by latency_ms) as p95,
            percentile_cont(0.99) within group (order by latency_ms) as p99
        from traffic_logs
        where tenant_id = :tenantId
          and created_at between :from and :to
        """)
    TrafficStats getStats(UUID tenantId, Instant from, Instant to);
}
```

**WHY native query для percentiles:** `percentile_cont` — PostgreSQL-specific функция. JPA/JPQL не поддерживает оконные функции. Это единственное место, где оправдан native query.

---

## 8. Security

### JWT Structure (custom claims)

```json
{
  "sub": "user-uuid",
  "iss": "http://localhost:8080",
  "aud": "gatekeeper",
  "tenant_id": "tenant-uuid",
  "roles": ["ADMIN", "OPERATOR"],
  "permissions": ["users:read", "routes:write"],
  "exp": 1723100000,
  "iat": 1723096400
}
```

**WHY permissions в JWT:** downstream-сервисы могут авторизовать запросы без обращения к GateKeeper. JWT самодостаточен. Trade-off: при изменении ролей нужно дождаться истечения токена (или revoke).

### JwtTokenCustomizer

```java
@Component
public class JwtTokenCustomizer implements OAuth2TokenCustomizer<JwtEncodingContext> {
    // добавляет tenant_id, roles, permissions в JWT claims
    // вызывается Spring Authorization Server при генерации токена
}
```

### Tenant Isolation

Каждый `@PreAuthorize` метод проверяет tenant scope через `TenantContext.getCurrentTenantId()`. Это ThreadLocal, заполняемый из JWT claim `tenant_id` в SecurityFilter.

**WHY ThreadLocal, а не передача tenantId параметром:** tenantId нужен на КАЖДОМ уровне (controller → service → repository). Передавать параметром — засорять сигнатуры. ThreadLocal + MDC дает доступ в любом месте стека.

---

## 9. Gateway Filter Chain

### Интерфейс

```java
public interface GatewayFilter {
    void doFilter(GatewayContext context, GatewayFilterChain chain);
}
```

### GatewayContext

```java
public class GatewayContext {
    HttpServletRequest request;
    HttpServletResponse response;
    GatewayRoute route;
    Map<String, Object> attributes;     // для передачи данных между фильтрами
    Instant startTime;
}
```

### Порядок фильтров

1. **IpFilter** — проверяет IP клиента по ip_rules тенанта. Blacklisted → 403.
2. **RateLimitFilter** — Bucket4j + Redis. Key: `rate:{tenantId}:{routeId}`. Exceeded → 429.
3. **AuthFilter** — если route.requireAuth, проверяет JWT из Authorization header + scopes.
4. **TransformationFilter** — добавляет/удаляет request headers per route config.
5. **(proxy call)**
6. **TrafficLoggingFilter** — после proxy, записывает TrafficLog (async через @Async).

**WHY свой FilterChain, а не javax.servlet.Filter:**
- Servlet Filter работает на уровне всего приложения. Нам нужны фильтры только для gateway proxy.
- Кастомная цепочка позволяет иметь GatewayContext с route-специфичными данными.
- Порядок фильтров контролируется явно (List в конфигурации), а не через @Order.

### ProxyService

```java
@Service
public class ProxyService {
    private final RestClient restClient;

    public ResponseEntity<byte[]> forward(GatewayContext context) {
        // 1. Построить target URL: route.targetUrl + stripped path
        // 2. Скопировать headers (кроме Host, Connection, Content-Length)
        // 3. Скопировать body (для POST/PUT/PATCH)
        // 4. Выполнить запрос через RestClient
        // 5. Вернуть response (status + headers + body)
    }
}
```

**WHY RestClient, а не WebClient:** приложение servlet-based. RestClient — синхронный HTTP-клиент из Spring 6.1, заменяет RestTemplate. WebClient добавляет reactor dependency без необходимости.

---

## 10. Caching

### Caffeine (in-process)

| Cache name | TTL | Что кешируется | Eviction |
|-----------|-----|----------------|----------|
| `routes` | 5 мин | GatewayRoute по tenantId | @CacheEvict при CRUD маршрутов |
| `ip-rules` | 5 мин | IpRule по tenantId | @CacheEvict при CRUD правил |
| `rate-configs` | 5 мин | RateLimitRule по tenantId | @CacheEvict при CRUD лимитов |

**WHY Caffeine, а не Redis для кеша:** маршруты и IP-правила читаются на КАЖДЫЙ proxy-запрос. Обращение к Redis (~0.5ms) на каждый запрос — ненужный overhead. Caffeine — in-process, <1μs. TTL 5 мин — приемлемая задержка для обновления конфигурации.

### Redis

Используется ТОЛЬКО для:
1. **Bucket4j state** — Token Bucket counters (per tenant + route). Нужен shared state между инстансами.
2. Больше ничего.

**WHY Redis только для rate limiting:** единственный use-case, где нужен shared mutable state. Все остальное либо в PostgreSQL (source of truth), либо в Caffeine (read-only cache).

---

## 11. Background Jobs

```java
@Configuration
@EnableScheduling
public class ScheduledJobs {

    @Scheduled(cron = "0 0 * * * *")  // каждый час
    void cleanExpiredSessions() {
        sessionRepository.deleteByExpiresAtBeforeAndRevokedFalse(Instant.now());
    }

    @Scheduled(cron = "0 0 3 * * *")  // ежедневно в 3:00
    void cleanOldTrafficLogs() {
        trafficLogRepository.deleteByCreatedAtBefore(Instant.now().minus(30, DAYS));
    }
}
```

**WHY @Scheduled, а не Quartz:** две простые периодические задачи. Quartz нужен для complex scheduling (retry, persistence, кластеризация). @Scheduled покрывает наши потребности с нулевыми зависимостями.

---

## 12. Error Handling

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    // NotFoundException → 404
    // ConflictException → 409
    // MethodArgumentNotValidException → 400 (field-level details)
    // AccessDeniedException → 403
    // AuthenticationException → 401
    // Exception → 500

    // Формат: ErrorResponse(timestamp, status, error, message, path)
}
```

**WHY единый GlobalExceptionHandler:** один класс определяет ВСЕ error responses. Гарантирует консистентный формат. Нет дублирования try/catch в контроллерах.

---

## 13. Logging

### Конфигурация

```xml
<!-- logback-spring.xml -->
<springProfile name="dev">
    <appender class="ConsoleAppender">
        <encoder class="PatternLayoutEncoder"/>   <!-- human-readable для dev -->
    </appender>
</springProfile>

<springProfile name="prod">
    <appender class="ConsoleAppender">
        <encoder class="LogstashEncoder"/>        <!-- structured JSON для prod -->
    </appender>
</springProfile>
```

### MDC Filter

```java
@Component
public class MdcFilter extends OncePerRequestFilter {
    // Устанавливает в MDC: requestId (UUID), tenantId, userId, clientIp
    // Очищает MDC в finally
}
```

**WHY MDC, а не передача параметрами:** MDC автоматически включает контекст в КАЖДУЮ строку лога без изменения сигнатур методов. Structured JSON логи с requestId позволяют трассировать весь flow одного запроса.

---

## 14. Testing

### Стратегия

```
Unit (Mockito)           — сервисы, фильтры, маппинг
Integration (Testcontainers) — API endpoints, repository queries, security flows
```

### AbstractIntegrationTest

```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
abstract class AbstractIntegrationTest {
    @Container static PostgreSQLContainer<?> postgres = ...;
    @Container static GenericContainer<?> redis = ...;

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.data.redis.host", redis::getHost);
    }
}
```

**WHY Testcontainers, а не H2/embedded Redis:** H2 не поддерживает PostgreSQL-specific функции (`percentile_cont`, partial indexes). Embedded Redis нестабилен. Testcontainers запускает реальные PostgreSQL и Redis — тесты проверяют настоящее поведение.

### Тестовые сценарии (ключевые)

- OAuth2 authorization code flow end-to-end
- Client credentials flow end-to-end
- RBAC: ADMIN может, VIEWER не может
- Tenant isolation: тенант A не видит данных тенанта B
- Gateway proxy: запрос проксируется к WireMock upstream
- Rate limiting: 429 при превышении лимита
- IP blacklist: 403 для blocked IP
- Idempotent filter chain execution
- Concurrent rate limiting (parallel requests)

---

## 15. Docker

### docker-compose.yml

```yaml
services:
  gatekeeper:
    build: .
    ports: ["8080:8080"]
    depends_on:
      postgres: { condition: service_healthy }
      redis: { condition: service_healthy }
    environment:
      SPRING_PROFILES_ACTIVE: dev
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/gatekeeper

  postgres:
    image: postgres:16-alpine
    ports: ["5432:5432"]
    volumes: [pgdata:/var/lib/postgresql/data]
    healthcheck: { test: "pg_isready -U gatekeeper" }

  redis:
    image: redis:7-alpine
    ports: ["6379:6379"]
    healthcheck: { test: "redis-cli ping" }

  echo-service:
    image: hashicorp/http-echo
    command: ["-text", '{"status":"ok"}']
    ports: ["9090:5678"]

volumes:
  pgdata:
```

**WHY echo-service:** для демонстрации gateway proxy. Можно создать маршрут `/echo/**` → `http://echo-service:5678` и увидеть проксирование в действии.

---

## 16. CI/CD (GitHub Actions)

```yaml
name: CI
on: [push, pull_request]
jobs:
  build-and-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: 21, distribution: temurin }
      - run: mvn verify -B          # compile + test (Testcontainers auto-detect CI)
      - uses: actions/upload-artifact@v4
        with: { name: test-report, path: target/surefire-reports/ }

  docker:
    needs: build-and-test
    if: github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: docker build -t gatekeeper .
```

---

## 17. Observability

### Prometheus Metrics (Micrometer)

```java
@Component
public class GatewayMetrics {
    private final Counter proxyRequests;     // gk_proxy_requests_total{route,method,status}
    private final Timer proxyLatency;        // gk_proxy_latency_seconds{route}
    private final Counter rateLimitRejects;  // gk_rate_limit_rejected_total
    private final Counter ipBlocked;         // gk_ip_blocked_total
}
```

### Health Checks

```
/actuator/health          — aggregate
/actuator/health/db       — PostgreSQL
/actuator/health/redis    — Redis
/actuator/prometheus      — metrics endpoint
```

### Key Dashboards (Prometheus queries)

```promql
# RPS через gateway
rate(gk_proxy_requests_total[5m])

# p95 latency
histogram_quantile(0.95, rate(gk_proxy_latency_seconds_bucket[5m]))

# rate limit rejections per minute
rate(gk_rate_limit_rejected_total[1m]) * 60
```
