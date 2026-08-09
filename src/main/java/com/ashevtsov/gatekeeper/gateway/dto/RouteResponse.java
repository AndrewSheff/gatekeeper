package com.ashevtsov.gatekeeper.gateway.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Ответ по маршруту — полная инфа включая трансформации
 */
public record RouteResponse(
        UUID id,
        UUID tenantId,
        String name,
        String predicatePath,
        String targetUrl,
        String methods,
        int stripPrefix,
        int orderPriority,
        boolean enabled,
        boolean requireAuth,
        String requiredScopes,
        List<TransformationResponse> transformations,
        Instant createdAt,
        Instant updatedAt
) {
    /**
     * Вложенный ответ по трансформации
     */
    public record TransformationResponse(
            UUID id,
            String type,
            String phase,
            String headerName,
            String headerValue
    ) {}
}
