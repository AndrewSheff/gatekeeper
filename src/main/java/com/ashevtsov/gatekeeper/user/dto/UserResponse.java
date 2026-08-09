package com.ashevtsov.gatekeeper.user.dto;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        String email,
        String firstName,
        String lastName,
        boolean enabled,
        UUID tenantId,
        Set<String> roles,
        Instant createdAt,
        Instant updatedAt
) {
}
