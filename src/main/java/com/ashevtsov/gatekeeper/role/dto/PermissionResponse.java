package com.ashevtsov.gatekeeper.role.dto;

import java.util.UUID;

public record PermissionResponse(
        UUID id,
        String name,
        String resource,
        String action,
        String description
) {
}
