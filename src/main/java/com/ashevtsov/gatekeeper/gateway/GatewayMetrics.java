package com.ashevtsov.gatekeeper.gateway;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Кастомные Prometheus-метрики для Gateway.
 * Считает проксированные запросы, rate limit реджекты, IP-блокировки, латентность.
 */
@Component
public class GatewayMetrics {

    private final MeterRegistry meterRegistry;

    private final Counter proxyRequestsTotal;
    private final Counter rateLimitRejectsTotal;
    private final Counter ipBlockedTotal;
    private final Counter proxyErrorsTotal;
    private final Timer proxyLatency;

    public GatewayMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        this.proxyRequestsTotal = Counter.builder("gatekeeper.proxy.requests.total")
                .description("Общее количество проксированных запросов")
                .register(meterRegistry);

        this.rateLimitRejectsTotal = Counter.builder("gatekeeper.ratelimit.rejects.total")
                .description("Запросы отклоненные rate limiter-ом")
                .register(meterRegistry);

        this.ipBlockedTotal = Counter.builder("gatekeeper.ip.blocked.total")
                .description("Запросы заблокированные IP-фильтром")
                .register(meterRegistry);

        this.proxyErrorsTotal = Counter.builder("gatekeeper.proxy.errors.total")
                .description("Ошибки при проксировании запросов")
                .register(meterRegistry);

        this.proxyLatency = Timer.builder("gatekeeper.proxy.latency")
                .description("Латентность проксирования запросов")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);
    }

    /** Запрос успешно проксирован */
    public void recordProxyRequest() {
        proxyRequestsTotal.increment();
    }

    /** Запрос отклонен rate limiter-ом */
    public void recordRateLimitReject() {
        rateLimitRejectsTotal.increment();
    }

    /** Запрос заблокирован IP-фильтром */
    public void recordIpBlocked() {
        ipBlockedTotal.increment();
    }

    /** Ошибка при проксировании */
    public void recordProxyError() {
        proxyErrorsTotal.increment();
    }

    /** Зафиксировать латентность запроса */
    public void recordLatency(Duration duration) {
        proxyLatency.record(duration);
    }

    /** Счетчик с кастомным тегом — для расширения */
    public void recordProxyRequest(String method, String path, int statusCode) {
        Counter.builder("gatekeeper.proxy.requests")
                .tag("method", method)
                .tag("status", String.valueOf(statusCode))
                .register(meterRegistry)
                .increment();
    }
}
