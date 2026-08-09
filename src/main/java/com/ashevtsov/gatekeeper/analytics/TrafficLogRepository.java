package com.ashevtsov.gatekeeper.analytics;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Репозиторий трафик-логов — кастомные запросы для аналитики.
 * Статистика считается прямо в базе — не тянем все записи в память.
 */
public interface TrafficLogRepository extends JpaRepository<TrafficLog, Long> {

    /**
     * Общее количество запросов за период для тенанта
     */
    long countByTenantIdAndCreatedAtBetween(UUID tenantId, Instant from, Instant to);

    /**
     * Средняя латентность за период — считаем в базе, не в Java
     */
    @Query("select coalesce(avg(t.latencyMs), 0) from TrafficLog t " +
           "where t.tenantId = :tenantId and t.createdAt >= :from and t.createdAt <= :to")
    double avgLatencyByTenantIdAndPeriod(UUID tenantId, Instant from, Instant to);

    /**
     * Количество ошибок (4xx + 5xx) за период
     */
    @Query("select count(t) from TrafficLog t " +
           "where t.tenantId = :tenantId and t.createdAt >= :from and t.createdAt <= :to " +
           "and t.statusCode >= 400")
    long countErrorsByTenantIdAndPeriod(UUID tenantId, Instant from, Instant to);

    /**
     * Топ путей по количеству запросов — для дашборда
     */
    @Query("select t.path, count(t) from TrafficLog t " +
           "where t.tenantId = :tenantId and t.createdAt >= :from and t.createdAt <= :to " +
           "group by t.path order by count(t) desc")
    List<Object[]> findTopPathsByTenantIdAndPeriod(UUID tenantId, Instant from, Instant to);
}
