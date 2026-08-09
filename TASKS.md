# GateKeeper — Implementation Plan

## Задачи

---

### GK-01: Инициализация проекта (Maven + Spring Boot)

**Описание:** Создать Maven-проект с Spring Boot 3.3, Java 21, подключить все зависимости из TDD. Настроить application.yml (dev/prod профили), logback-spring.xml, pom.xml. Создать GatekeeperApplication.java.

**Зависимости:** нет

**Файлы:**
- `pom.xml`
- `src/main/java/.../GatekeeperApplication.java`
- `src/main/resources/application.yml`
- `src/main/resources/application-dev.yml`
- `src/main/resources/logback-spring.xml`

**Критерии готовности:**
- Проект компилируется (`mvn compile`)
- Приложение стартует и отвечает на `/actuator/health`
- Все зависимости из TDD подключены

**Тесты:** smoke test — контекст поднимается

**Сложность:** S

---

### GK-02: Docker Compose (PostgreSQL + Redis + echo-service)

**Описание:** Создать Dockerfile (multi-stage) и docker-compose.yml с PostgreSQL, Redis, echo-service. Приложение подключается к БД и Redis.

**Зависимости:** GK-01

**Файлы:**
- `docker/Dockerfile`
- `docker-compose.yml`
- `.dockerignore`

**Критерии готовности:**
- `docker-compose up` поднимает все 4 сервиса
- Приложение стартует, подключается к PostgreSQL и Redis
- echo-service отвечает на http://localhost:9090

**Тесты:** нет (инфраструктура)

**Сложность:** S

---

### GK-03: Common — exceptions, DTO, GlobalExceptionHandler

**Описание:** Создать пакет `common/`: ErrorResponse, PageResponse, GatekeeperException, NotFoundException, ConflictException, GlobalExceptionHandler. MDC-фильтр для requestId.

**Зависимости:** GK-01

**Файлы:**
- `common/dto/ErrorResponse.java`
- `common/dto/PageResponse.java`
- `common/exception/*.java`
- `config/MdcFilter.java`

**Критерии готовности:**
- GlobalExceptionHandler маппит все exception-типы в ErrorResponse
- MDC-фильтр добавляет requestId в каждый запрос
- PageResponse обобщенно оборачивает Page<T>

**Тесты:**
- Unit: GlobalExceptionHandler возвращает правильные HTTP-коды
- Unit: PageResponse корректно маппит Spring Page

**Сложность:** S

---

### GK-04: Liquibase — миграции БД

**Описание:** Создать все Liquibase changesets (001-010) из TDD: tenants, users, roles, permissions, join tables, OAuth2 tables, gateway_routes, ip_rules, rate_limit_rules, traffic_logs, audit_logs, user_sessions, seed data.

**Зависимости:** GK-02

**Файлы:**
- `src/main/resources/db/changelog/db.changelog-master.xml`
- `src/main/resources/db/changelog/001-create-tenants.xml`
- `...010-seed-data.xml`

**Критерии готовности:**
- Приложение стартует, Liquibase создает все таблицы
- Seed data: default tenant, SUPER_ADMIN user, базовые permissions
- Индексы созданы по TDD

**Тесты:** Integration: проверка что все таблицы существуют после старта

**Сложность:** M

---

### GK-05: Tenant — entity, repository, service, controller

**Описание:** Реализовать домен Tenant: entity, repository, service (CRUD), controller (/api/v1/tenants), mapper (MapStruct), request/response DTO с валидацией. Генерация API-ключа при создании.

**Зависимости:** GK-03, GK-04

**Файлы:**
- `tenant/Tenant.java`
- `tenant/TenantRepository.java`
- `tenant/TenantService.java`
- `tenant/TenantController.java`
- `tenant/TenantMapper.java`
- `tenant/dto/*.java`

**Критерии готовности:**
- CRUD работает: POST/GET/GET(id)/PUT
- API-ключ генерируется при создании (UUID-based, prefix `gk_live_`)
- Валидация: name обязателен, slug уникален
- Swagger UI показывает все endpoints

**Тесты:**
- Unit: TenantService (create, update, duplicate slug → ConflictException)
- Integration: TenantController (CRUD flow, validation errors)

**Сложность:** M

---

### GK-06: User — entity, repository, service, controller

**Описание:** Домен User: entity (с BCrypt password), repository, service, controller, mapper. CRUD с привязкой к tenant. Пагинация и поиск.

**Зависимости:** GK-05

**Файлы:**
- `user/User.java`
- `user/UserRepository.java`
- `user/UserService.java`
- `user/UserController.java`
- `user/UserMapper.java`
- `user/dto/*.java`

**Критерии готовности:**
- CRUD /api/v1/users
- Пароль хешируется BCrypt при создании
- Поиск по username/email (query param `search`)
- Пагинация (page, size, sort)
- Soft delete (enabled=false)

**Тесты:**
- Unit: UserService (create, duplicate username, BCrypt)
- Integration: UserController (CRUD, search, pagination)

**Сложность:** M

---

### GK-07: Role + Permission — entities, service, controller

**Описание:** Домены Role и Permission: entities, repositories, services, controllers. Иерархия ролей (parent). Метод getEffectivePermissions (рекурсивный сбор permissions от parent-ов, maxDepth=5).

**Зависимости:** GK-05

**Файлы:**
- `role/Role.java`, `role/Permission.java`
- `role/RoleRepository.java`, `role/PermissionRepository.java`
- `role/RoleService.java`
- `role/RoleController.java`
- `role/RoleMapper.java`
- `role/dto/*.java`

**Критерии готовности:**
- CRUD /api/v1/roles с permissions assignment
- getEffectivePermissions собирает permissions по иерархии
- Permissions — справочные данные (CRUD)
- Роль уникальна в рамках тенанта

**Тесты:**
- Unit: getEffectivePermissions (3-уровневая иерархия, защита от цикла)
- Integration: RoleController (CRUD, permissions assignment)

**Сложность:** M

---

### GK-08: Spring Security — JWT auth + RBAC

**Описание:** Настроить SecurityConfig: 3 SecurityFilterChain (auth server, admin API, gateway). CustomUserDetailsService для загрузки User + roles. JwtTokenCustomizer для добавления tenantId, roles, permissions в JWT claims. TenantContext (ThreadLocal). @PreAuthorize на контроллерах.

**Зависимости:** GK-06, GK-07

**Файлы:**
- `config/SecurityConfig.java`
- `security/CustomUserDetailsService.java`
- `security/JwtTokenCustomizer.java`
- `security/TenantContext.java`

**Критерии готовности:**
- /api/v1/** требует JWT Bearer
- JWT содержит tenantId, roles, permissions
- @PreAuthorize("hasAuthority('users:read')") работает
- TenantContext доступен во всех сервисах
- Неавторизованный запрос → 401, запрещенный → 403

**Тесты:**
- Integration: доступ с JWT → 200, без JWT → 401, без прав → 403
- Integration: tenant isolation (тенант A не видит данных тенанта B)

**Сложность:** L

---

### GK-09: Spring Authorization Server — OAuth2/OIDC

**Описание:** Настроить AuthorizationServerConfig: RSA ключи, JdbcRegisteredClientRepository, token settings. Endpoints: /oauth2/token, /oauth2/authorize, /oauth2/revoke, /oauth2/introspect, /oauth2/jwks, /.well-known/openid-configuration.

**Зависимости:** GK-08

**Файлы:**
- `config/AuthorizationServerConfig.java`

**Критерии готовности:**
- Client Credentials flow: POST /oauth2/token → access token
- Authorization Code flow работает end-to-end
- Refresh token flow работает
- Token revocation работает
- JWKS endpoint возвращает публичные ключи
- OIDC Discovery endpoint возвращает корректный JSON

**Тесты:**
- Integration: client credentials flow E2E
- Integration: authorization code flow E2E
- Integration: refresh token flow
- Integration: token revocation

**Сложность:** L

---

### GK-10: OAuth2 Client Management — service, controller

**Описание:** OAuthClientService (обертка над RegisteredClientRepository) + OAuthClientController для CRUD OAuth2-клиентов через REST API.

**Зависимости:** GK-09

**Файлы:**
- `client/OAuthClientService.java`
- `client/OAuthClientController.java`
- `client/OAuthClientMapper.java`
- `client/dto/*.java`

**Критерии готовности:**
- POST /api/v1/clients — регистрация (secret показывается один раз)
- GET /api/v1/clients — список
- PUT /api/v1/clients/{id} — обновление (scopes, redirect URIs)
- DELETE /api/v1/clients/{id} — деактивация

**Тесты:**
- Integration: register client → get token с новым client_id → success

**Сложность:** M

---

### GK-11: Session Management

**Описание:** UserSession entity + repository + service + controller. Просмотр активных сессий, отзыв сессии.

**Зависимости:** GK-09

**Файлы:**
- `session/UserSession.java`
- `session/UserSessionRepository.java`
- `session/SessionService.java`
- `session/SessionController.java`

**Критерии готовности:**
- GET /api/v1/sessions — активные сессии текущего пользователя
- DELETE /api/v1/sessions/{id} — отзыв сессии
- @Scheduled очистка истекших сессий

**Тесты:**
- Integration: создание сессии при token issue, просмотр, отзыв

**Сложность:** S

---

### GK-12: Gateway Routes — CRUD + RouteResolver

**Описание:** GatewayRoute entity + RouteTransformation entity + repository + RouteService + RouteController. RouteResolver — резолв маршрута по path с Caffeine кешем. AntPathMatcher для matching.

**Зависимости:** GK-05, GK-08

**Файлы:**
- `gateway/GatewayRoute.java`
- `gateway/RouteTransformation.java`
- `gateway/GatewayRouteRepository.java`
- `gateway/RouteService.java`
- `gateway/RouteController.java`
- `gateway/RouteResolver.java`
- `gateway/RouteMapper.java`
- `gateway/dto/*.java`
- `config/CacheConfig.java`

**Критерии готовности:**
- CRUD /api/v1/routes (с transformations)
- RouteResolver.resolve(path) возвращает маршрут или null
- Caffeine кеш с TTL 5 мин, @CacheEvict при CRUD
- AntPathMatcher: `/users-service/**` матчит `/users-service/api/users`

**Тесты:**
- Unit: RouteResolver (matching, priority, cache eviction)
- Integration: RouteController CRUD

**Сложность:** M

---

### GK-13: Gateway Filter Chain + ProxyController

**Описание:** Ядро gateway: GatewayFilter interface, GatewayFilterChain, GatewayContext, ProxyController (catch-all `/**`), ProxyService (RestClient forward). Без фильтров — только proxy.

**Зависимости:** GK-12

**Файлы:**
- `gateway/filter/GatewayFilter.java`
- `gateway/filter/GatewayFilterChain.java`
- `gateway/filter/GatewayContext.java`
- `gateway/ProxyController.java`
- `gateway/ProxyService.java`
- `config/RestClientConfig.java`

**Критерии готовности:**
- Запрос к `/users-service/api/users` проксируется к target URL
- RestClient forward: headers, body, method копируются
- 404 если маршрут не найден
- Strip prefix работает

**Тесты:**
- Integration: proxy к echo-service через WireMock
- Unit: ProxyService (header copy, path stripping)

**Сложность:** L

---

### GK-14: Gateway Filters — IP, RateLimit, Auth, Transformation, Logging

**Описание:** Реализовать 5 фильтров: IpFilter (whitelist/blacklist + CIDR), RateLimitFilter (Bucket4j + Redis), AuthFilter (JWT scope check), TransformationFilter (headers), TrafficLoggingFilter (async write TrafficLog).

**Зависимости:** GK-13

**Файлы:**
- `gateway/filter/IpFilter.java`
- `gateway/filter/RateLimitFilter.java`
- `gateway/filter/AuthFilter.java`
- `gateway/filter/TransformationFilter.java`
- `gateway/filter/TrafficLoggingFilter.java`
- `config/RedisConfig.java`

**Критерии готовности:**
- IpFilter: blacklisted IP → 403, CIDR matching работает
- RateLimitFilter: Bucket4j + Redis, 429 при превышении, Retry-After header
- AuthFilter: проверка JWT scope для route.requiredScopes
- TransformationFilter: ADD/REMOVE/SET headers per route config
- TrafficLoggingFilter: async запись в traffic_logs

**Тесты:**
- Unit: IpFilter (CIDR matching, whitelist/blacklist logic)
- Unit: RateLimitFilter (bucket creation, rate exceeded)
- Integration: полный flow через все фильтры
- Integration: rate limiting с Redis (Testcontainers)

**Сложность:** L

---

### GK-15: IP Rules + Rate Limits — CRUD controllers

**Описание:** IpRule entity + IpRuleController + IpFilterService. RateLimitRule entity + RateLimitController + RateLimitService.

**Зависимости:** GK-14

**Файлы:**
- `ipfilter/IpRule.java`, `ipfilter/IpRuleRepository.java`
- `ipfilter/IpFilterService.java`, `ipfilter/IpRuleController.java`
- `ratelimit/RateLimitRule.java`, `ratelimit/RateLimitRepository.java`
- `ratelimit/RateLimitService.java`, `ratelimit/RateLimitController.java`

**Критерии готовности:**
- CRUD /api/v1/ip-rules, CRUD /api/v1/rate-limits
- Кеширование Caffeine + eviction при CRUD

**Тесты:**
- Integration: create IP rule → blocked request

**Сложность:** M

---

### GK-16: Analytics — TrafficLog + AnalyticsController

**Описание:** TrafficLog entity + repository (native query с percentile_cont). AnalyticsService + AnalyticsController (/api/v1/analytics/overview, /routes/{id}).

**Зависимости:** GK-14

**Файлы:**
- `analytics/TrafficLog.java`, `analytics/TrafficLogRepository.java`
- `analytics/AnalyticsService.java`, `analytics/AnalyticsController.java`
- `analytics/dto/TrafficOverview.java`, `analytics/dto/RouteStats.java`

**Критерии готовности:**
- GET /api/v1/analytics/overview — total requests, RPS, p50/p95/p99
- GET /api/v1/analytics/routes/{id} — per-route stats
- Фильтрация по периоду (from/to)

**Тесты:**
- Integration: send N requests through proxy → analytics returns correct stats

**Сложность:** M

---

### GK-17: Audit Log

**Описание:** AuditLog entity + repository + AuditService (@Async). Интеграция: вызов auditService.log() из всех сервисов при CRUD-операциях.

**Зависимости:** GK-05

**Файлы:**
- `audit/AuditLog.java`, `audit/AuditLogRepository.java`
- `audit/AuditService.java`

**Критерии готовности:**
- Все CRUD-операции записывают аудит (action, entityType, entityId, details JSONB)
- @Async — не блокирует основной flow

**Тесты:**
- Integration: create user → audit log entry exists

**Сложность:** S

---

### GK-18: Prometheus Metrics + OpenAPI

**Описание:** Настроить Micrometer custom metrics (proxy requests, latency, rate limit rejects, IP blocked). Настроить SpringDoc OpenAPI (группировка по тегам). Финальный health check.

**Зависимости:** GK-14, GK-16

**Файлы:**
- `gateway/GatewayMetrics.java`
- `config/OpenApiConfig.java`

**Критерии готовности:**
- /actuator/prometheus — все custom метрики из TDD
- Swagger UI — все endpoints сгруппированы и описаны
- Health checks: db + redis

**Тесты:**
- Integration: verify metrics endpoint contains custom metrics

**Сложность:** S

---

### GK-19: CI/CD (GitHub Actions)

**Описание:** Создать .github/workflows/ci.yml: build, test (Testcontainers), docker build.

**Зависимости:** GK-18

**Файлы:**
- `.github/workflows/ci.yml`

**Критерии готовности:**
- Pipeline: checkout → setup Java 21 → mvn verify → docker build
- Тесты проходят в CI (Testcontainers auto-detect)

**Тесты:** нет (инфраструктура)

**Сложность:** S

---

## Dependency Graph

```
GK-01 ──► GK-02 ──► GK-04 ──┐
  │                          │
  └──► GK-03 ────────────────┼──► GK-05 ──┬──► GK-06 ──┐
                             │            │            │
                             │            ├──► GK-07 ──┤
                             │            │            │
                             │            └──► GK-12   │
                             │                 │       │
                             │                 │  GK-08 ◄┘
                             │                 │    │
                             │                 │  GK-09
                             │                 │    │
                             │                 │  ┌─┴──┐
                             │                 │  │    │
                             │            GK-10   GK-11
                             │                 │
                             │           GK-13 ◄┘
                             │                 │
                             │           GK-14
                             │            │  │
                             │       GK-15   GK-16
                             │                 │
                             │           GK-18 ◄┘
                             │                 │
                             │           GK-19
                             │
                        GK-05 ──► GK-17
```

## Порядок реализации

```
GK-01 → GK-02 → GK-03 → GK-04 → GK-05 → GK-06 → GK-07 →
GK-08 → GK-09 → GK-10 → GK-11 → GK-12 → GK-13 → GK-14 →
GK-15 → GK-16 → GK-17 → GK-18 → GK-19
```

**Итого: 19 задач** (4S + 8M + 4L + 3S = 5 Small, 8 Medium, 4 Large)
