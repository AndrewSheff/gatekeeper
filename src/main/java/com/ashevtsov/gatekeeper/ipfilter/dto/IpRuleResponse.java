package com.ashevtsov.gatekeeper.ipfilter.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Ответ с данными IP-правила — все что клиенту нужно знать
 */
public record IpRuleResponse(
        UUID id,
        UUID tenantId,
        String ipPattern,
        String ruleType,
        String description,
        Instant createdAt
) {
}
