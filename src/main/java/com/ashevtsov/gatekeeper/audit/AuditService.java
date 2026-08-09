package com.ashevtsov.gatekeeper.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Сервис аудита — пишет записи асинхронно, чтобы не тормозить основной flow.
 * Вызывается из других сервисов при CRUD-операциях.
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Записываем аудит-событие асинхронно — не блокируем вызывающий поток.
     * Если что-то пойдет не так, залогируем ошибку, но не уроним основной запрос.
     */
    @Async
    @Transactional
    public void log(UUID tenantId, UUID userId, String action, String entityType,
                    String entityId, String details, String ipAddress) {
        try {
            var auditLog = new AuditLog();
            auditLog.setTenantId(tenantId);
            auditLog.setUserId(userId);
            auditLog.setAction(action);
            auditLog.setEntityType(entityType);
            auditLog.setEntityId(entityId);
            auditLog.setDetails(details);
            auditLog.setIpAddress(ipAddress);

            auditLogRepository.save(auditLog);
            log.debug("Аудит: {} {} {} (tenant={})", action, entityType, entityId, tenantId);
        } catch (Exception e) {
            // аудит не должен ронять основную логику — просто логируем ошибку
            log.error("Не удалось записать аудит: {} {} {}", action, entityType, entityId, e);
        }
    }
}
