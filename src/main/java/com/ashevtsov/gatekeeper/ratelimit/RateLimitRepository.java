package com.ashevtsov.gatekeeper.ratelimit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Репозиторий rate limit правил — выборка по тенанту и маршруту
 */
public interface RateLimitRepository extends JpaRepository<RateLimitRule, UUID> {

    /**
     * Все правила тенанта — для админки
     */
    List<RateLimitRule> findByTenantId(UUID tenantId);

    /**
     * Активные правила для конкретного маршрута или глобальные (route is null)
     */
    List<RateLimitRule> findByTenantIdAndEnabledTrue(UUID tenantId);

    /**
     * Правила привязанные к конкретному маршруту
     */
    List<RateLimitRule> findByRouteId(UUID routeId);
}
