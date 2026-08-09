package com.ashevtsov.gatekeeper.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

public record CreateUserRequest(
        @NotBlank(message = "Username обязателен")
        @Size(min = 3, max = 50, message = "Username от 3 до 50 символов")
        String username,

        @NotBlank(message = "Email обязателен")
        @Email(message = "Невалидный email")
        String email,

        @NotBlank(message = "Пароль обязателен")
        @Size(min = 6, message = "Минимум 6 символов")
        String password,

        String firstName,
        String lastName,

        @NotNull(message = "tenantId обязателен")
        UUID tenantId,

        Set<UUID> roleIds
) {
}
