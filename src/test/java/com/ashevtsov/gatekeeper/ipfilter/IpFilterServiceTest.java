package com.ashevtsov.gatekeeper.ipfilter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Тесты IP-фильтрации — CIDR-матчинг, blacklist/whitelist логика
 */
@ExtendWith(MockitoExtension.class)
class IpFilterServiceTest {

    @Mock
    IpRuleRepository ipRuleRepository;

    IpFilterService ipFilterService;

    UUID tenantId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        ipFilterService = new IpFilterService(ipRuleRepository);
    }

    @Test
    void noRules_allowsAll() {
        when(ipRuleRepository.findByTenantId(tenantId)).thenReturn(List.of());

        assertTrue(ipFilterService.isAllowed("1.2.3.4", tenantId));
    }

    @Test
    void blacklist_blocksMatchingIp() {
        var rule = createRule("10.0.0.0/8", "BLACKLIST");
        when(ipRuleRepository.findByTenantId(tenantId)).thenReturn(List.of(rule));

        assertFalse(ipFilterService.isAllowed("10.5.3.1", tenantId));
        // не в этом диапазоне — пропускаем
        assertTrue(ipFilterService.isAllowed("192.168.1.1", tenantId));
    }

    @Test
    void whitelist_blocksNonMatchingIp() {
        var rule = createRule("192.168.1.0/24", "WHITELIST");
        when(ipRuleRepository.findByTenantId(tenantId)).thenReturn(List.of(rule));

        assertTrue(ipFilterService.isAllowed("192.168.1.50", tenantId));
        assertFalse(ipFilterService.isAllowed("10.0.0.1", tenantId));
    }

    @Test
    void exactIp_matchesWithoutCidr() {
        var rule = createRule("8.8.8.8", "BLACKLIST");
        when(ipRuleRepository.findByTenantId(tenantId)).thenReturn(List.of(rule));

        assertFalse(ipFilterService.isAllowed("8.8.8.8", tenantId));
        assertTrue(ipFilterService.isAllowed("8.8.8.9", tenantId));
    }

    @Test
    void cidrMatching_variousPrefixes() {
        // /32 — точное совпадение
        assertTrue(ipFilterService.matchesCidr("192.168.1.1", "192.168.1.1/32"));
        assertFalse(ipFilterService.matchesCidr("192.168.1.2", "192.168.1.1/32"));

        // /24 — подсеть класса C
        assertTrue(ipFilterService.matchesCidr("192.168.1.100", "192.168.1.0/24"));
        assertFalse(ipFilterService.matchesCidr("192.168.2.1", "192.168.1.0/24"));

        // /16 — подсеть класса B
        assertTrue(ipFilterService.matchesCidr("172.16.50.1", "172.16.0.0/16"));
        assertFalse(ipFilterService.matchesCidr("172.17.0.1", "172.16.0.0/16"));
    }

    @Test
    void invalidIp_returnsFalse() {
        assertFalse(ipFilterService.matchesCidr("not-an-ip", "192.168.1.0/24"));
    }

    private IpRule createRule(String pattern, String type) {
        var rule = new IpRule();
        rule.setIpPattern(pattern);
        rule.setRuleType(type);
        rule.setDescription("тестовое правило");
        return rule;
    }
}
