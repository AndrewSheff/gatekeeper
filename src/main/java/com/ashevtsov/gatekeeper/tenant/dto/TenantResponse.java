package com.ashevtsov.gatekeeper.tenant.dto;

import java.time.Instant;
import java.util.UUID;

public record TenantResponse(
        UUID id,
        String name,
        String slug,
        String apiKey,
        boolean enabled,
        int maxRps,
        Instant createdAt,
        Instant updatedAt
) {
}
