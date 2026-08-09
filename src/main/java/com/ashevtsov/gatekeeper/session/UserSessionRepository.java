package com.ashevtsov.gatekeeper.session;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Репозиторий сессий — ищем активные, чистим просроченные
 */
public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {

    /**
     * Активные (не отозванные) сессии конкретного пользователя
     */
    List<UserSession> findByUserIdAndRevokedFalse(UUID userId);

    /**
     * Удаляем просроченные сессии, которые еще не были отозваны — для scheduled-очистки
     */
    void deleteByExpiresAtBeforeAndRevokedFalse(Instant now);
}
