package com.ashevtsov.gatekeeper.role.dto;

import java.util.Set;
import java.util.UUID;

public record UpdateRoleRequest(
        String name,
        String description,
        UUID parentId,
        Set<UUID> permissionIds
) {
}
