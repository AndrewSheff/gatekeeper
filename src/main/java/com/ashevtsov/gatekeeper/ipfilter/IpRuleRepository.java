package com.ashevtsov.gatekeeper.ipfilter;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Репозиторий IP-правил — достаем по тенанту для фильтрации
 */
public interface IpRuleRepository extends JpaRepository<IpRule, UUID> {

    /**
     * Все правила тенанта — для проверки в IpFilterService
     */
    List<IpRule> findByTenantId(UUID tenantId);
}
