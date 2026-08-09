package com.ashevtsov.gatekeeper.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.List;
import java.util.UUID;

/**
 * Резолвер маршрутов — по входящему пути и тенанту находит нужный GatewayRoute.
 * Использует AntPathMatcher для матчинга (всякие /users-service/** и подобное).
 * Результат кешируется в Caffeine — зачем лезть в БД на каждый proxy-запрос.
 */
@Component
public class RouteResolver {

    private static final Logger log = LoggerFactory.getLogger(RouteResolver.class);
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private final GatewayRouteRepository routeRepository;

    public RouteResolver(GatewayRouteRepository routeRepository) {
        this.routeRepository = routeRepository;
    }

    /**
     * Находим подходящий маршрут для пути. Перебираем по приоритету — первый матч побеждает.
     * Кешируем по tenantId (маршруты одного тенанта меняются редко).
     */
    @Cacheable(value = "routes", key = "#tenantId")
    public List<GatewayRoute> getRoutes(UUID tenantId) {
        return routeRepository.findByTenantIdAndEnabledTrueOrderByOrderPriorityAsc(tenantId);
    }

    /**
     * Резолвим конкретный маршрут по пути — достаем из кеша список и матчим AntPathMatcher-ом
     */
    public GatewayRoute resolve(String path, UUID tenantId) {
        var routes = getRoutes(tenantId);

        for (var route : routes) {
            if (pathMatcher.match(route.getPredicatePath(), path)) {
                log.debug("Путь '{}' заматчился на маршрут '{}'", path, route.getName());
                return route;
            }
        }

        log.debug("Маршрут для пути '{}' не найден (тенант {})", path, tenantId);
        return null;
    }
}
