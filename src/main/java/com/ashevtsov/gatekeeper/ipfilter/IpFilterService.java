package com.ashevtsov.gatekeeper.ipfilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.UUID;

/**
 * Сервис проверки IP-адресов — основная логика фильтрации.
 * Поддерживает CIDR-нотацию (10.0.0.0/8, 192.168.1.0/24) и отдельные IP.
 * Логика: если есть blacklist и IP туда попал — блокируем. Если есть whitelist,
 * но IP в него не входит — тоже блокируем. Результат кешируется на 5 минут.
 */
@Service
@Transactional(readOnly = true)
public class IpFilterService {

    private static final Logger log = LoggerFactory.getLogger(IpFilterService.class);

    private final IpRuleRepository ipRuleRepository;

    public IpFilterService(IpRuleRepository ipRuleRepository) {
        this.ipRuleRepository = ipRuleRepository;
    }

    /**
     * Проверяем — можно ли пускать этот IP для данного тенанта.
     * Кеш "ip-rules" — не дергаем базу на каждый запрос.
     */
    @Cacheable(value = "ip-rules", key = "#tenantId")
    public boolean isAllowed(String clientIp, UUID tenantId) {
        List<IpRule> rules = ipRuleRepository.findByTenantId(tenantId);

        if (rules.isEmpty()) {
            // нет правил — пускаем всех
            return true;
        }

        List<IpRule> blacklistRules = rules.stream()
                .filter(r -> "BLACKLIST".equalsIgnoreCase(r.getRuleType()))
                .toList();

        List<IpRule> whitelistRules = rules.stream()
                .filter(r -> "WHITELIST".equalsIgnoreCase(r.getRuleType()))
                .toList();

        // проверяем блеклист — если попал, сразу в отказ
        for (IpRule rule : blacklistRules) {
            if (matchesCidr(clientIp, rule.getIpPattern())) {
                log.info("IP {} заблокирован по blacklist-правилу '{}' (tenant={})",
                        clientIp, rule.getDescription(), tenantId);
                return false;
            }
        }

        // если есть вайтлист, проверяем что IP в нем
        if (!whitelistRules.isEmpty()) {
            boolean inWhitelist = whitelistRules.stream()
                    .anyMatch(rule -> matchesCidr(clientIp, rule.getIpPattern()));

            if (!inWhitelist) {
                log.info("IP {} не найден в whitelist (tenant={})", clientIp, tenantId);
                return false;
            }
        }

        return true;
    }

    /**
     * Проверяем — попадает ли IP в CIDR-диапазон.
     * Поддерживает формат "192.168.1.0/24" и обычные IP "10.0.0.1".
     * Если паттерн без маски — считаем /32 (точное совпадение).
     */
    boolean matchesCidr(String clientIp, String cidrPattern) {
        try {
            String networkPart;
            int prefixLength;

            if (cidrPattern.contains("/")) {
                String[] parts = cidrPattern.split("/");
                networkPart = parts[0];
                prefixLength = Integer.parseInt(parts[1]);
            } else {
                // обычный IP без маски — точное совпадение
                networkPart = cidrPattern;
                prefixLength = 32;
            }

            byte[] clientBytes = InetAddress.getByName(clientIp).getAddress();
            byte[] networkBytes = InetAddress.getByName(networkPart).getAddress();

            // длина адреса не совпадает (IPv4 vs IPv6) — точно не матчится
            if (clientBytes.length != networkBytes.length) {
                return false;
            }

            // побитово сравниваем с учетом маски
            int fullBytes = prefixLength / 8;
            int remainingBits = prefixLength % 8;

            // сравниваем полные байты
            for (int i = 0; i < fullBytes; i++) {
                if (clientBytes[i] != networkBytes[i]) {
                    return false;
                }
            }

            // сравниваем оставшиеся биты если есть
            if (remainingBits > 0 && fullBytes < clientBytes.length) {
                int mask = 0xFF << (8 - remainingBits);
                if ((clientBytes[fullBytes] & mask) != (networkBytes[fullBytes] & mask)) {
                    return false;
                }
            }

            return true;
        } catch (UnknownHostException | NumberFormatException e) {
            log.warn("Не удалось распарсить CIDR '{}' или IP '{}': {}", cidrPattern, clientIp, e.getMessage());
            return false;
        }
    }
}
