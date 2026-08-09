package com.ashevtsov.gatekeeper.session;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Управление сессиями — просмотр активных и принудительный отзыв.
 * Полезно когда пользователь хочет выкинуть чужую сессию.
 */
@RestController
@RequestMapping("/api/v1/sessions")
@Tag(name = "Sessions", description = "Управление сессиями пользователей")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @GetMapping
    @Operation(summary = "Активные сессии пользователя")
    public List<UserSession> getActiveSessions(@RequestParam UUID userId) {
        return sessionService.getActiveSessions(userId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Отозвать сессию")
    public void revokeSession(@PathVariable UUID id) {
        sessionService.revokeSession(id);
    }
}
