package com.ashevtsov.gatekeeper.ipfilter.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

/**
 * Запрос на создание IP-правила — валидируем все обязательные поля
 */
public record CreateIpRuleRequest(

        @NotNull(message = "tenantId обязателен")
        UUID tenantId,

        @NotBlank(message = "ipPattern обязателен")
        String ipPattern,

        @NotBlank(message = "ruleType обязателен")
        @Pattern(regexp = "^(WHITELIST|BLACKLIST)$", message = "ruleType должен быть WHITELIST или BLACKLIST")
        String ruleType,

        String description
) {
}
