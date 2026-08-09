package com.ashevtsov.gatekeeper.gateway;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Репозиторий маршрутов — достаем по тенанту, с фильтром по enabled
 */
public interface GatewayRouteRepository extends JpaRepository<GatewayRoute, UUID> {

    /**
     * Активные маршруты тенанта — для RouteResolver, сортированные по приоритету
     */
    List<GatewayRoute> findByTenantIdAndEnabledTrueOrderByOrderPriorityAsc(UUID tenantId);

    /**
     * Все маршруты тенанта (включая отключенные) — для админки
     */
    Page<GatewayRoute> findByTenantId(UUID tenantId, Pageable pageable);
}
