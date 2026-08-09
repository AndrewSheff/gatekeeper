# GateKeeper — Product Requirements Document

## 1. Цель

GateKeeper — централизованный сервис аутентификации, авторизации и управления API-трафиком для микросервисных архитектур. Объединяет функциональность Identity Provider (Keycloak) и API Gateway (Kong) в одном продукте. Решает проблему разрозненности инфраструктуры безопасности: вместо настройки и поддержки двух отдельных систем компания получает единую точку управления доступом и маршрутизацией.

**Бизнес-проблема:** компании, переходящие на микросервисы, вынуждены настраивать Keycloak + Kong/Nginx + кастомную авторизацию. Это дорого, сложно в поддержке и создает точки отказа. GateKeeper дает единое решение с OAuth2/OIDC + API Gateway + rate limiting + аналитику.

---

## 2. Целевые пользователи

| Роль | Описание | Задачи |
|------|----------|--------|
| **Системный администратор** | Управляет инфраструктурой | Настройка тенантов, глобальные политики, мониторинг |
| **DevOps-инженер** | Управляет маршрутизацией | Настройка маршрутов, rate limits, IP-правила, деплой |
| **Тимлид / Архитектор** | Управляет доступом команд | Регистрация OAuth2-клиентов, назначение scopes |
| **Backend-разработчик** | Интегрирует свои сервисы | Получение токенов, настройка клиентов, тестирование API |
| **Security-инженер** | Аудит и безопасность | Просмотр аудит-логов, управление сессиями, IP-политики |

**Типичная компания-клиент:** IT-компания 50-500 разработчиков, 10-50 микросервисов, переход с монолита или замена связки Keycloak + Nginx.

---

## 3. Бизнес-сценарии

### SC-1: Онбординг нового микросервиса
1. DevOps создает маршрут в GateKeeper (`/api/orders/**` → `http://order-service:8080`)
2. Тимлид регистрирует OAuth2-клиент для сервиса (client credentials flow)
3. Разработчик получает client_id + client_secret
4. Сервис запрашивает access token через `POST /oauth2/token`
5. Сервис обращается к другим сервисам через gateway с Bearer-токеном
6. Gateway валидирует токен, проверяет scopes, проксирует запрос

### SC-2: SSO для веб-приложения
1. Тимлид регистрирует OAuth2-клиент с authorization_code flow
2. Пользователь открывает веб-приложение → редирект на GateKeeper `/oauth2/authorize`
3. Пользователь вводит логин/пароль → GateKeeper выдает authorization code
4. Приложение обменивает code на access + refresh токены
5. При истечении access token — автоматическое обновление через refresh token
6. Пользователь может просмотреть и отозвать свои сессии

### SC-3: Защита от DDoS / злоупотреблений
1. DevOps настраивает rate limit: 100 RPS на тенант, 50 RPS на маршрут `/api/payments`
2. Входящие запросы проходят через Token Bucket фильтр
3. При превышении лимита — ответ 429 Too Many Requests с заголовком Retry-After
4. Security-инженер добавляет IP 185.x.x.x в blacklist
5. Все запросы с этого IP блокируются до удаления правила

### SC-4: Аудит инцидента безопасности
1. Security-инженер замечает аномальный трафик в Prometheus
2. Открывает `/api/v1/analytics/overview` — видит пик RPS на маршруте `/api/users`
3. Смотрит аудит-лог — находит массовые запросы от client_id `compromised-app`
4. Отзывает клиент через `DELETE /api/v1/clients/{id}`
5. Добавляет IP-адрес атакующего в blacklist
6. Все сессии автоматически инвалидируются

### SC-5: Мультитенантность для SaaS
1. Админ создает тенант "Acme Corp" с лимитом 200 RPS
2. Создает пользователей и роли внутри тенанта
3. Настраивает маршруты и IP-правила для тенанта
4. Данные изолированы — тенант "Acme" не видит данные тенанта "Beta"
5. Каждый тенант имеет свой API-ключ для идентификации

---

## 4. Функциональные требования

### FR-1: OAuth2 Authorization Server
- Authorization Code Flow (для веб-приложений)
- Client Credentials Flow (для сервис-к-сервису)
- Refresh Token Flow (обновление токенов)
- Token Revocation (отзыв токенов)
- Token Introspection (проверка валидности)
- OIDC Discovery (`/.well-known/openid-configuration`)
- JWKS endpoint (публичные ключи для верификации JWT)

### FR-2: Управление пользователями
- CRUD пользователей с пагинацией и поиском
- Назначение ролей пользователю
- Деактивация (soft delete) вместо удаления
- Привязка к тенанту

### FR-3: RBAC с иерархией
- Создание ролей с parent-ролью (наследование permissions)
- Гранулярные permissions (resource + action: `users:read`, `routes:write`)
- Предустановленные роли: SUPER_ADMIN, ADMIN, OPERATOR, VIEWER

### FR-4: Управление OAuth2-клиентами
- Регистрация клиентов с выбором grant types
- Настройка scopes, redirect URIs, TTL токенов
- Генерация client_secret (показывается один раз)
- Деактивация клиента

### FR-5: Мультитенантность
- Изоляция данных по tenant_id
- API-ключ для идентификации тенанта
- Глобальные лимиты RPS на тенант
- SUPER_ADMIN управляет всеми тенантами, ADMIN — только своим

### FR-6: API Gateway (reverse proxy)
- Динамические маршруты из БД (path → target URL)
- Поддержка методов GET/POST/PUT/PATCH/DELETE
- Strip prefix (удаление N сегментов пути)
- Приоритизация маршрутов (order_priority)
- Включение/отключение маршрутов без удаления

### FR-7: Rate Limiting
- Token Bucket алгоритм через Redis
- Настройка per tenant (глобальный) и per route
- Заголовки `X-RateLimit-Remaining`, `X-RateLimit-Reset`, `Retry-After`
- Burst capacity для кратковременных пиков

### FR-8: IP Filtering
- Whitelist (только разрешенные IP)
- Blacklist (блокировка конкретных IP)
- Поддержка CIDR-нотации (192.168.1.0/24)
- Per tenant

### FR-9: Request/Response трансформация
- Добавление/удаление/замена заголовков (request и response)
- Автоподстановка `X-Tenant-Id`, `X-Request-Id`
- Настройка трансформаций per route

### FR-10: Аналитика трафика
- Общая статистика: RPS, latency percentiles (p50/p95/p99), breakdown по status codes
- Статистика per route
- Фильтрация по периоду

### FR-11: Session Management
- Просмотр активных сессий пользователя (IP, User-Agent, время создания)
- Отзыв конкретной сессии
- Автоочистка истекших сессий

### FR-12: Аудит
- Append-only лог всех административных действий
- Кто, когда, что сделал, какие данные изменились (JSONB)

---

## 5. Нефункциональные требования

| Требование | Целевое значение |
|------------|-----------------|
| Latency gateway proxy (p95) | <50ms overhead |
| Latency auth endpoints (p95) | <100ms |
| Throughput | 1000+ RPS на gateway |
| Доступность | 99.9% (архитектурно — stateless JWT, горизонтальное масштабирование) |
| Время старта | <10s (с Liquibase-миграциями) |
| Покрытие тестами | >80% line coverage |
| Безопасность | OWASP Top 10 compliant |
| Совместимость | Java 21+, PostgreSQL 16+, Redis 7+ |
| Docker-образ | <200MB (alpine-based) |
| Документация API | 100% endpoints в Swagger UI |

---

## 6. API

### Группы эндпоинтов

| Префикс | Назначение | Auth |
|---------|-----------|------|
| `/oauth2/*` | OAuth2 протокол | По стандарту OAuth2 |
| `/.well-known/*` | OIDC Discovery | Public |
| `/api/v1/users` | Управление пользователями | ADMIN+ |
| `/api/v1/roles` | Управление ролями | ADMIN+ |
| `/api/v1/clients` | Управление OAuth2-клиентами | ADMIN+ |
| `/api/v1/tenants` | Управление тенантами | SUPER_ADMIN / ADMIN |
| `/api/v1/sessions` | Управление сессиями | Bearer (свои) |
| `/api/v1/routes` | Управление маршрутами | ADMIN+ |
| `/api/v1/ip-rules` | IP Whitelist/Blacklist | ADMIN+ |
| `/api/v1/rate-limits` | Rate limiting | ADMIN+ |
| `/api/v1/analytics` | Аналитика трафика | ADMIN+ |
| `/**` (catch-all) | Gateway proxy | По настройке маршрута |

Итого: ~34 кастомных + 6 OAuth2 = **40 эндпоинтов**

### Формат ответов

Успех (single): `{ "id": "uuid", "field": "value", ... }`
Успех (list): `{ "content": [...], "page": 0, "size": 20, "totalElements": N, "totalPages": N }`
Ошибка: `{ "timestamp": "ISO", "status": 404, "error": "Not Found", "message": "...", "path": "/..." }`

---

## 7. Роли и права

### Иерархия ролей

```
SUPER_ADMIN (управление всеми тенантами)
└── ADMIN (управление своим тенантом)
    └── OPERATOR (управление маршрутами, IP-правилами)
        └── VIEWER (только просмотр)
```

### Матрица доступа

| Ресурс | SUPER_ADMIN | ADMIN | OPERATOR | VIEWER |
|--------|-------------|-------|----------|--------|
| Тенанты (CRUD) | CRUD | RU (свой) | R | R |
| Пользователи | CRUD | CRUD (свой тенант) | R | R |
| Роли | CRUD | CRUD (свой тенант) | R | R |
| Клиенты | CRUD | CRUD (свой тенант) | R | R |
| Маршруты | CRUD | CRUD | CRUD | R |
| IP-правила | CRUD | CRUD | CRUD | R |
| Rate limits | CRUD | CRUD | CRUD | R |
| Аналитика | R | R | R | R |
| Аудит-лог | R | R | - | - |

---

## 8. Обработка ошибок

| HTTP-код | Когда | Пример |
|----------|-------|--------|
| 400 | Невалидный запрос | `"email: must be a valid email address"` |
| 401 | Не аутентифицирован | Отсутствует / невалидный токен |
| 403 | Нет прав | VIEWER пытается создать пользователя |
| 404 | Ресурс не найден | Пользователь с таким ID не существует |
| 409 | Конфликт | Username уже занят |
| 429 | Rate limit | Превышен лимит RPS, заголовок Retry-After |
| 500 | Внутренняя ошибка | Недоступна БД / Redis |
| 502 | Upstream error | Gateway не смог связаться с upstream-сервисом |
| 504 | Upstream timeout | Upstream не ответил за 30 секунд |

---

## 9. Безопасность

- **Хеширование паролей:** BCrypt (cost factor 12)
- **JWT подпись:** RS256 (RSA 2048-bit), ключевая пара генерируется при первом старте
- **Access Token TTL:** 1 час (настраивается per client)
- **Refresh Token TTL:** 24 часа (настраивается per client)
- **Custom JWT Claims:** `tenant_id`, `roles`, `permissions` — для авторизации на стороне downstream-сервисов
- **CORS:** настраиваемый список origins
- **CSRF:** отключен (stateless API)
- **Rate limiting:** защита от brute force на `/oauth2/token`
- **IP filtering:** блокировка подозрительных IP
- **Audit log:** все административные действия логируются
- **Secrets:** client_secret показывается один раз при создании, хранится в BCrypt
- **Input validation:** все входные данные валидируются через Jakarta Validation
- **SQL Injection:** параметризованные запросы через JPA
- **Нет PII в логах:** пароли, токены, секреты не логируются

---

## 10. Хранение данных

| Хранилище | Назначение | Данные |
|-----------|-----------|--------|
| PostgreSQL 16 | Основное хранилище | Пользователи, роли, клиенты, маршруты, аудит |
| Redis 7 | Кеш + rate limiting | Token Bucket state, кеш маршрутов/IP-правил |
| Caffeine | Локальный кеш | Маршруты (TTL 5 мин), конфигурация (TTL 5 мин) |

**Миграции:** Liquibase, 10 changesets (tenants → users/roles → oauth2 → routes → ip_rules → rate_limits → traffic_logs → audit_logs → sessions → seed data)

**Индексы:** по tenant_id на всех tenant-scoped таблицах, по created_at на логах, partial index на user_sessions (where revoked = false)

---

## 11. Интеграции

| Интеграция | Протокол | Назначение |
|-----------|----------|-----------|
| Upstream-сервисы | HTTP/REST | Gateway проксирует запросы |
| Redis | RESP | Rate limiting state, кеширование |
| Prometheus | HTTP (pull) | Сбор метрик через `/actuator/prometheus` |
| Любой OAuth2-клиент | OAuth2/OIDC | Аутентификация (стандартный протокол) |

---

## 12. Фоновые задачи

| Задача | Расписание | Описание |
|--------|-----------|----------|
| Очистка истекших сессий | Каждый час | Удаление `user_sessions` где `expires_at < now()` |
| Очистка старых traffic_logs | Ежедневно, 3:00 | Удаление записей старше 30 дней |
| Ротация RSA-ключей | — | Ручная, через конфигурацию |
| Кеш-инвалидация | По событию | При CRUD маршрутов/IP-правил — evict Caffeine |

Реализация: `@Scheduled` (Spring) для периодических задач.

---

## 13. Логирование

- **Формат:** Structured JSON (Logback + LogstashEncoder)
- **MDC-контекст:** `requestId` (UUID), `tenantId`, `userId`, `clientIp`
- **Уровни:**
  - ERROR: сбои БД/Redis, ошибки proxy, необработанные исключения
  - WARN: rate limit exceeded, IP blocked, невалидный токен
  - INFO: создание/удаление ресурсов, OAuth2 token выдан
  - DEBUG: детали HTTP-запросов, кеш hit/miss
- **Не логировать:** пароли, токены, client_secret, тело запроса (кроме DEBUG)

---

## 14. Мониторинг

### Prometheus-метрики

| Метрика | Тип | Описание |
|---------|-----|----------|
| `gk_proxy_requests_total` | Counter | Запросы через gateway (labels: route, method, status) |
| `gk_proxy_latency_seconds` | Histogram | Latency проксирования (labels: route) |
| `gk_rate_limit_rejected_total` | Counter | Отклонено rate limiter-ом |
| `gk_ip_blocked_total` | Counter | Заблокировано IP-фильтром |
| `gk_auth_tokens_issued_total` | Counter | Выданные токены (labels: grant_type) |
| `gk_active_sessions_count` | Gauge | Количество активных сессий |

### Health Checks

- `/actuator/health` — общий статус
- `/actuator/health/db` — PostgreSQL
- `/actuator/health/redis` — Redis

---

## 15. Тестирование

| Тип | Кол-во | Описание |
|-----|--------|----------|
| Unit | ~30 | Сервисы, фильтры, маппинг (Mockito) |
| Integration | ~30 | API endpoints, репозитории (Testcontainers) |
| Security | ~15 | OAuth2 flows, RBAC, token validation |
| Gateway | ~15 | Proxy, rate limiting, IP filtering (WireMock) |
| **Итого** | **~90** | |

Базовый класс `AbstractIntegrationTest` с Testcontainers (PostgreSQL + Redis), переиспользуется во всех IT-тестах.

---

## 16. Docker

| Сервис | Образ | Порт |
|--------|-------|------|
| gatekeeper | eclipse-temurin:21-jre-alpine | 8080 |
| postgres | postgres:16-alpine | 5432 |
| redis | redis:7-alpine | 6379 |
| echo-service | hashicorp/http-echo | 9090 |

Multi-stage Dockerfile: `maven:3.9-eclipse-temurin-21-alpine` (build) → `eclipse-temurin:21-jre-alpine` (run).

---

## 17. CI/CD (GitHub Actions)

```
on: [push, pull_request]
jobs:
  build:    mvn compile
  test:     mvn verify (Testcontainers)
  lint:     checkstyle
  security: OWASP dependency-check
  docker:   build + push (на main)
```

---

## 18. Критерии готовности

- [ ] Все 34 кастомных эндпоинта реализованы и задокументированы в Swagger UI
- [ ] OAuth2 Authorization Code + Client Credentials flows работают end-to-end
- [ ] RBAC с иерархией: SUPER_ADMIN → ADMIN → OPERATOR → VIEWER
- [ ] Gateway проксирует запросы к echo-service через динамические маршруты из БД
- [ ] Rate limiting (Token Bucket) отклоняет запросы при превышении лимита
- [ ] IP whitelist/blacklist блокирует/пропускает запросы
- [ ] Multi-tenant: данные изолированы между тенантами
- [ ] 90+ тестов, >80% покрытие
- [ ] Docker Compose поднимает полную среду одной командой
- [ ] CI pipeline проходит (build + test + lint)
- [ ] Structured JSON logging с MDC-контекстом
- [ ] Prometheus-метрики экспортируются
- [ ] README с описанием, quick start, API-примерами
