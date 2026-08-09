package com.ashevtsov.gatekeeper.tenant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record CreateTenantRequest(
        @NotBlank(message = "Название обязательно")
        String name,

        @NotBlank(message = "Slug обязателен")
        @Pattern(regexp = "^[a-z0-9-]+$", message = "Slug должен содержать только строчные буквы, цифры и дефис")
        String slug,

        @Positive(message = "maxRps должен быть положительным")
        Integer maxRps
) {
    public CreateTenantRequest {
        if (maxRps == null) maxRps = 100;
    }
}
