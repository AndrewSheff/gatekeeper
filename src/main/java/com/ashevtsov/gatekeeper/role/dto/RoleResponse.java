package com.ashevtsov.gatekeeper.role.dto;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record RoleResponse(
        UUID id,
        String name,
        String description,
        UUID parentId,
        UUID tenantId,
        Set<PermissionResponse> permissions,
        Instant createdAt
) {
}
