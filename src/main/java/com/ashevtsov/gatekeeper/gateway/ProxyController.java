package com.ashevtsov.gatekeeper.gateway;

import com.ashevtsov.gatekeeper.gateway.filter.GatewayContext;
import com.ashevtsov.gatekeeper.gateway.filter.GatewayFilter;
import com.ashevtsov.gatekeeper.gateway.filter.GatewayFilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Catch-all контроллер — ловит все запросы которые не ушли в /api/v1/** и /oauth2/**,
 * прогоняет через цепочку фильтров и проксирует на target-сервис.
 * @Order(LOWEST_PRECEDENCE) чтоб не перехватывать чужие маршруты.
 */
@RestController
@Order(Ordered.LOWEST_PRECEDENCE)
public class ProxyController {

    private static final Logger log = LoggerFactory.getLogger(ProxyController.class);

    /**
     * Дефолтный тенант — пока нет мультитенантной резолюции из запроса,
     * используем фиксированный UUID. Потом заменим на реальный резолв по домену/хедеру.
     */
    private static final UUID DEFAULT_TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    /** Префиксы которые не трогаем — пусть Spring сам разруливает */
    private static final List<String> RESERVED_PREFIXES = List.of("/api/", "/oauth2/");

    private final RouteResolver routeResolver;
    private final ProxyService proxyService;
    private final List<GatewayFilter> filters;

    public ProxyController(RouteResolver routeResolver,
                           ProxyService proxyService,
                           List<GatewayFilter> filters) {
        this.routeResolver = routeResolver;
        this.proxyService = proxyService;

        // сортируем фильтры по @Order — чтоб RateLimit шел раньше Auth и т.д.
        this.filters = new ArrayList<>(filters);
        AnnotationAwareOrderComparator.sort(this.filters);

        log.info("ProxyController запущен, фильтров в цепочке: {}", this.filters.size());
    }

    /**
     * Главный обработчик — ловит все что осталось после конкретных маппингов.
     * Проверяет что путь не зарезервированный, резолвит маршрут, прогоняет фильтры, проксирует.
     */
    @RequestMapping("/**")
    public ResponseEntity<?> handleProxy(HttpServletRequest request, HttpServletResponse response) {
        String path = request.getRequestURI();

        // зарезервированные пути — не наше дело, пусть Spring рулит
        for (String prefix : RESERVED_PREFIXES) {
            if (path.startsWith(prefix)) {
                log.debug("Путь '{}' зарезервирован, пропускаем", path);
                return ResponseEntity.notFound().build();
            }
        }

        // пока тенанта берем дефолтный, потом будет резолв по домену/хедеру
        UUID tenantId = DEFAULT_TENANT_ID;

        // ищем маршрут
        GatewayRoute route = routeResolver.resolve(path, tenantId);
        if (route == null) {
            log.debug("Маршрут для '{}' не найден, отдаем 404", path);
            return ResponseEntity.notFound().build();
        }

        // собираем контекст
        GatewayContext context = new GatewayContext(request, response, route);
        context.setAttribute("tenantId", tenantId);

        // прогоняем цепочку фильтров
        GatewayFilterChain chain = new GatewayFilterChain(filters);
        chain.doFilter(context);

        // если фильтр уже записал ответ (например, заблокировал запрос) — не проксируем
        if (response.isCommitted()) {
            log.debug("Ответ уже committed фильтром, проксирование отменено");
            return null;
        }

        // проксируем на target-сервис
        return proxyService.forward(context);
    }
}
