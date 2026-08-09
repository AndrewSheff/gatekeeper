package com.ashevtsov.gatekeeper.tenant.dto;

import jakarta.validation.constraints.Positive;

public record UpdateTenantRequest(
        String name,
        Boolean enabled,
        @Positive(message = "maxRps должен быть положительным")
        Integer maxRps
) {
}
