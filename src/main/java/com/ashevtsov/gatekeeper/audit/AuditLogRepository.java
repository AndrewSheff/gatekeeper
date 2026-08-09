package com.ashevtsov.gatekeeper.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.UUID;

/**
 * Репозиторий аудит-логов — в основном запись, но иногда нужно посмотреть что натворили
 */
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * Логи тенанта за период — для просмотра аудита
     */
    Page<AuditLog> findByTenantIdAndCreatedAtBetween(UUID tenantId, Instant from, Instant to, Pageable pageable);

    /**
     * Логи по тенанту — без фильтра по дате
     */
    Page<AuditLog> findByTenantId(UUID tenantId, Pageable pageable);
}
