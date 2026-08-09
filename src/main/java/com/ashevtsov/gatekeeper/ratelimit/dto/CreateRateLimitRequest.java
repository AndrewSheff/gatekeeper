package com.ashevtsov.gatekeeper.ratelimit.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

/**
 * Запрос на создание rate limit правила — routeId опционален (глобальный лимит для тенанта)
 */
public record CreateRateLimitRequest(

        @NotNull(message = "tenantId обязателен")
        UUID tenantId,

        /** Если null — правило действует на весь тенант */
        UUID routeId,

        @NotNull(message = "requestsPerSecond обязателен")
        @Positive(message = "requestsPerSecond должен быть положительным")
        Integer requestsPerSecond,

        @NotNull(message = "burstCapacity обязателен")
        @Positive(message = "burstCapacity должен быть положительным")
        Integer burstCapacity,

        Boolean enabled
) {
    public CreateRateLimitRequest {
        // по умолчанию правило включено при создании
        if (enabled == null) enabled = true;
    }
}
