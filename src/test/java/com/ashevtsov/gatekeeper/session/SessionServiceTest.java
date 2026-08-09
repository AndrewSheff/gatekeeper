package com.ashevtsov.gatekeeper.session;

import com.ashevtsov.gatekeeper.common.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Тесты сервиса сессий — отзыв, получение активных
 */
@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    @Mock
    UserSessionRepository sessionRepository;

    SessionService sessionService;

    @BeforeEach
    void setUp() {
        sessionService = new SessionService(sessionRepository);
    }

    @Test
    void getActiveSessions_returnsList() {
        var userId = UUID.randomUUID();
        var session = new UserSession();
        session.setId(UUID.randomUUID());
        session.setRevoked(false);
        session.setExpiresAt(Instant.now().plusSeconds(3600));

        when(sessionRepository.findByUserIdAndRevokedFalse(userId)).thenReturn(List.of(session));

        var result = sessionService.getActiveSessions(userId);
        assertEquals(1, result.size());
        assertFalse(result.get(0).isRevoked());
    }

    @Test
    void revokeSession_setsRevokedTrue() {
        var sessionId = UUID.randomUUID();
        var session = new UserSession();
        session.setId(sessionId);
        session.setRevoked(false);

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        sessionService.revokeSession(sessionId);

        assertTrue(session.isRevoked());
        verify(sessionRepository).save(session);
    }

    @Test
    void revokeSession_notFound_throwsNotFoundException() {
        var sessionId = UUID.randomUUID();
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> sessionService.revokeSession(sessionId));
    }

    @Test
    void cleanExpired_callsDeleteOnRepository() {
        sessionService.cleanExpired();
        verify(sessionRepository).deleteByExpiresAtBeforeAndRevokedFalse(any(Instant.class));
    }
}
