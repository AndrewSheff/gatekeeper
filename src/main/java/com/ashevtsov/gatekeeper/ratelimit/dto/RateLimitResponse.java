package com.ashevtsov.gatekeeper.ratelimit.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Ответ с данными rate limit правила
 */
public record RateLimitResponse(
        UUID id,
        UUID tenantId,
        UUID routeId,
        int requestsPerSecond,
        int burstCapacity,
        boolean enabled,
        Instant createdAt
) {
}
