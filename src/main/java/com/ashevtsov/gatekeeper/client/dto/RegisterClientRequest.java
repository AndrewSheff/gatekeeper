package com.ashevtsov.gatekeeper.client.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

public record RegisterClientRequest(
        @NotBlank(message = "clientName обязателен")
        String clientName,

        @NotEmpty(message = "Хотя бы один grant type обязателен")
        Set<String> grantTypes,

        Set<String> redirectUris,
        Set<String> scopes,

        Long accessTokenTtlSeconds,
        Long refreshTokenTtlSeconds
) {
    public RegisterClientRequest {
        if (scopes == null || scopes.isEmpty()) scopes = Set.of("read");
        if (accessTokenTtlSeconds == null) accessTokenTtlSeconds = 3600L;
        if (refreshTokenTtlSeconds == null) refreshTokenTtlSeconds = 86400L;
    }
}
