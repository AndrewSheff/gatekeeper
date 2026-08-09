package com.ashevtsov.gatekeeper.analytics;

import com.ashevtsov.gatekeeper.analytics.dto.TrafficOverview;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Сервис аналитики — собирает обзор трафика за период.
 * Все считается в базе через JPQL — в память тянем только результат.
 */
@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    private static final int TOP_PATHS_LIMIT = 10;

    private final TrafficLogRepository trafficLogRepository;

    public AnalyticsService(TrafficLogRepository trafficLogRepository) {
        this.trafficLogRepository = trafficLogRepository;
    }

    /**
     * Обзор трафика для дашборда — основные метрики за период.
     * Делаем несколько запросов в базу, но они легкие (агрегаты с индексами).
     */
    public TrafficOverview getOverview(UUID tenantId, Instant from, Instant to) {
        long totalRequests = trafficLogRepository.countByTenantIdAndCreatedAtBetween(tenantId, from, to);
        double avgLatency = trafficLogRepository.avgLatencyByTenantIdAndPeriod(tenantId, from, to);
        long errorCount = trafficLogRepository.countErrorsByTenantIdAndPeriod(tenantId, from, to);

        double errorRate = totalRequests > 0 ? (double) errorCount / totalRequests * 100.0 : 0.0;

        // топ путей — берем все из базы и обрезаем до лимита
        List<Object[]> topPathsRaw = trafficLogRepository.findTopPathsByTenantIdAndPeriod(tenantId, from, to);
        List<TrafficOverview.PathStats> topPaths = topPathsRaw.stream()
                .limit(TOP_PATHS_LIMIT)
                .map(row -> new TrafficOverview.PathStats((String) row[0], (Long) row[1]))
                .toList();

        return new TrafficOverview(totalRequests, avgLatency, errorCount, errorRate, topPaths, from, to);
    }
}
