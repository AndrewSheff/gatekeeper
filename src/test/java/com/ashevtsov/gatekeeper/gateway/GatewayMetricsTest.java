package com.ashevtsov.gatekeeper.gateway;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты кастомных метрик — убеждаемся что счетчики работают
 */
class GatewayMetricsTest {

    MeterRegistry meterRegistry;
    GatewayMetrics gatewayMetrics;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        gatewayMetrics = new GatewayMetrics(meterRegistry);
    }

    @Test
    void proxyRequests_incrementsCounter() {
        gatewayMetrics.recordProxyRequest();
        gatewayMetrics.recordProxyRequest();

        var counter = meterRegistry.find("gatekeeper.proxy.requests.total").counter();
        assertNotNull(counter);
        assertEquals(2.0, counter.count());
    }

    @Test
    void rateLimitRejects_incrementsCounter() {
        gatewayMetrics.recordRateLimitReject();

        var counter = meterRegistry.find("gatekeeper.ratelimit.rejects.total").counter();
        assertNotNull(counter);
        assertEquals(1.0, counter.count());
    }

    @Test
    void ipBlocked_incrementsCounter() {
        gatewayMetrics.recordIpBlocked();

        var counter = meterRegistry.find("gatekeeper.ip.blocked.total").counter();
        assertNotNull(counter);
        assertEquals(1.0, counter.count());
    }

    @Test
    void latency_recordsTimer() {
        gatewayMetrics.recordLatency(Duration.ofMillis(150));
        gatewayMetrics.recordLatency(Duration.ofMillis(250));

        var timer = meterRegistry.find("gatekeeper.proxy.latency").timer();
        assertNotNull(timer);
        assertEquals(2, timer.count());
    }

    @Test
    void taggedProxyRequest_createsCounterWithTags() {
        gatewayMetrics.recordProxyRequest("GET", "/api/test", 200);

        var counter = meterRegistry.find("gatekeeper.proxy.requests")
                .tag("method", "GET")
                .tag("status", "200")
                .counter();
        assertNotNull(counter);
        assertEquals(1.0, counter.count());
    }
}
