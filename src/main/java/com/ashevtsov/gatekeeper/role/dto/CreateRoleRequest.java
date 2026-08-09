package com.ashevtsov.gatekeeper.role.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Set;
import java.util.UUID;

public record CreateRoleRequest(
        @NotBlank(message = "Название роли обязательно")
        String name,
        String description,
        UUID parentId,
        @NotNull(message = "tenantId обязателен")
        UUID tenantId,
        Set<UUID> permissionIds
) {
}
