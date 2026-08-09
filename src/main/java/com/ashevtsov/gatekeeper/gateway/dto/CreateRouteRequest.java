package com.ashevtsov.gatekeeper.gateway.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Запрос на создание маршрута — все обязательные поля + опциональные трансформации
 */
public record CreateRouteRequest(
        @NotNull(message = "tenantId обязателен")
        UUID tenantId,

        @NotBlank(message = "Название маршрута обязательно")
        String name,

        @NotBlank(message = "predicatePath обязателен")
        String predicatePath,

        @NotBlank(message = "targetUrl обязателен")
        String targetUrl,

        String methods,
        Integer stripPrefix,
        Integer orderPriority,
        Boolean enabled,
        Boolean requireAuth,
        String requiredScopes,
        List<TransformationDto> transformations
) {
    public CreateRouteRequest {
        if (stripPrefix == null) stripPrefix = 1;
        if (orderPriority == null) orderPriority = 0;
        if (enabled == null) enabled = true;
        if (requireAuth == null) requireAuth = true;
    }

    /**
     * DTO для трансформации в составе запроса
     */
    public record TransformationDto(
            @NotBlank(message = "type обязателен")
            String type,
            @NotBlank(message = "phase обязателен")
            String phase,
            @NotBlank(message = "headerName обязателен")
            String headerName,
            String headerValue
    ) {}
}
