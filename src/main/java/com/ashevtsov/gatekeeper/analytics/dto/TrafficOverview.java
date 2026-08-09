package com.ashevtsov.gatekeeper.analytics.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Обзор трафика за период — основные метрики для дашборда.
 * Сколько запросов, средняя латентность, процент ошибок, топ-пути.
 */
public record TrafficOverview(
        /** Общее количество запросов */
        long totalRequests,

        /** Средняя латентность в миллисекундах */
        double avgLatencyMs,

        /** Количество ошибок (4xx + 5xx) */
        long errorCount,

        /** Процент ошибок от общего кол-ва запросов */
        double errorRate,

        /** Топ-10 путей по количеству запросов */
        List<PathStats> topPaths,

        /** Начало периода */
        Instant from,

        /** Конец периода */
        Instant to
) {
    /**
     * Статистика по конкретному пути — путь + количество запросов
     */
    public record PathStats(String path, long requestCount) {
    }
}
