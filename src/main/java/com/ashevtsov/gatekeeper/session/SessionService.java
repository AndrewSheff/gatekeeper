package com.ashevtsov.gatekeeper.session;

import com.ashevtsov.gatekeeper.common.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Управление сессиями: просмотр активных, отзыв, очистка просроченных.
 * Ничего хитрого — просто CRUD с небольшой логикой.
 */
@Service
@Transactional(readOnly = true)
public class SessionService {

    private static final Logger log = LoggerFactory.getLogger(SessionService.class);

    private final UserSessionRepository sessionRepository;

    public SessionService(UserSessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    /**
     * Все активные сессии пользователя — те что не отозваны
     */
    public List<UserSession> getActiveSessions(UUID userId) {
        return sessionRepository.findByUserIdAndRevokedFalse(userId);
    }

    /**
     * Отзыв конкретной сессии — помечаем revoked=true
     */
    @Transactional
    public void revokeSession(UUID sessionId) {
        var session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("UserSession", sessionId));

        session.setRevoked(true);
        sessionRepository.save(session);

        log.info("Сессия {} отозвана", sessionId);
    }

    /**
     * Чистим просроченные сессии — вызывается по расписанию
     */
    @Transactional
    public void cleanExpired() {
        sessionRepository.deleteByExpiresAtBeforeAndRevokedFalse(Instant.now());
        log.info("Просроченные сессии вычищены");
    }
}
