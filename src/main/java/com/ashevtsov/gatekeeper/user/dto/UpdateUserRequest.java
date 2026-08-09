package com.ashevtsov.gatekeeper.user.dto;

import jakarta.validation.constraints.Email;

import java.util.Set;
import java.util.UUID;

public record UpdateUserRequest(
        @Email(message = "Невалидный email")
        String email,
        String firstName,
        String lastName,
        Boolean enabled,
        Set<UUID> roleIds
) {
}
