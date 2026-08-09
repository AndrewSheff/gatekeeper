package com.ashevtsov.gatekeeper.ipfilter;

import com.ashevtsov.gatekeeper.tenant.Tenant;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Правило IP-фильтрации — белый/черный список айпишников для тенанта.
 * ipPattern поддерживает как отдельные адреса (192.168.1.1), так и CIDR (10.0.0.0/8).
 * ruleType = WHITELIST | BLACKLIST — определяет что с этим IP делать.
 */
@Entity
@Table(name = "ip_rules")
public class IpRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    /** IP-адрес или CIDR-нотация (например, "192.168.1.0/24") */
    @Column(name = "ip_pattern", nullable = false)
    private String ipPattern;

    /** WHITELIST или BLACKLIST */
    @Column(name = "rule_type", nullable = false, length = 20)
    private String ruleType;

    /** Описание — зачем добавили это правило */
    @Column(length = 500)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    // --- геттеры и сеттеры ---

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Tenant getTenant() { return tenant; }
    public void setTenant(Tenant tenant) { this.tenant = tenant; }

    public String getIpPattern() { return ipPattern; }
    public void setIpPattern(String ipPattern) { this.ipPattern = ipPattern; }

    public String getRuleType() { return ruleType; }
    public void setRuleType(String ruleType) { this.ruleType = ruleType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Instant getCreatedAt() { return createdAt; }
}
