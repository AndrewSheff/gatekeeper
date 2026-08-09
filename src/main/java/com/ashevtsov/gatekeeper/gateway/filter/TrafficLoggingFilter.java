package com.ashevtsov.gatekeeper.gateway.filter;

import com.ashevtsov.gatekeeper.analytics.TrafficLog;
import com.ashevtsov.gatekeeper.analytics.TrafficLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Логирование трафика — последний фильтр в цепочке (Order=100).
 * После chain.doFilter() записывает traffic log асинхронно, чтобы не тормозить ответ клиенту.
 * Фиксирует латентность от GatewayContext.startTime до момента завершения цепочки.
 */
@Component
@Order(100)
public class TrafficLoggingFilter implements GatewayFilter {

    private static final Logger log = LoggerFactory.getLogger(TrafficLoggingFilter.class);

    private final TrafficLogRepository trafficLogRepository;

    public TrafficLoggingFilter(TrafficLogRepository trafficLogRepository) {
        this.trafficLogRepository = trafficLogRepository;
    }

    @Override
    public void doFilter(GatewayContext context, GatewayFilterChain chain) {
        // пропускаем дальше по цепочке (или к ProxyService)
        chain.doFilter(context);

        // считаем латентность
        long latencyMs = Duration.between(context.getStartTime(), Instant.now()).toMillis();

        // достаем данные из контекста
        UUID tenantId = context.getAttribute("tenantId");
        UUID routeId = context.getRoute().getId();
        String method = context.getRequest().getMethod();
        String path = context.getRequest().getRequestURI();
        int statusCode = context.getResponse().getStatus();
        String clientIp = context.getRequest().getRemoteAddr();
        String requestId = context.getRequest().getHeader("X-Request-Id");

        // записываем асинхронно — не блокируем ответ клиенту
        saveTrafficLogAsync(tenantId, routeId, method, path, statusCode, latencyMs, clientIp, requestId);
    }

    /**
     * Сохраняем лог трафика асинхронно — если упадет, логируем ошибку, но не ломаем основной flow
     */
    @Async
    public void saveTrafficLogAsync(UUID tenantId, UUID routeId, String method,
                                     String path, int statusCode, long latencyMs,
                                     String clientIp, String requestId) {
        try {
            var trafficLog = new TrafficLog();
            trafficLog.setTenantId(tenantId);
            trafficLog.setRouteId(routeId);
            trafficLog.setMethod(method);
            trafficLog.setPath(path);
            trafficLog.setStatusCode(statusCode);
            trafficLog.setLatencyMs(latencyMs);
            trafficLog.setClientIp(clientIp);
            trafficLog.setRequestId(requestId);

            trafficLogRepository.save(trafficLog);

            log.debug("TrafficLog: {} {} -> {} ({}ms, tenant={})",
                    method, path, statusCode, latencyMs, tenantId);
        } catch (Exception e) {
            // трафик-лог не должен ронять основной запрос — молча проглатываем
            log.error("Не удалось записать traffic log: {} {}", method, path, e);
        }
    }
}
