package com.ashevtsov.gatekeeper.ratelimit;

import com.ashevtsov.gatekeeper.gateway.GatewayRoute;
import com.ashevtsov.gatekeeper.tenant.Tenant;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Правило rate limit — ограничение частоты запросов для тенанта и/или маршрута.
 * Если route == null, правило действует глобально на весь тенант.
 * requestsPerSecond — базовая скорость, burstCapacity — пиковый всплеск.
 */
@Entity
@Table(name = "rate_limit_rules")
public class RateLimitRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    /** Маршрут — если null, правило действует на весь тенант */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id")
    private GatewayRoute route;

    /** Допустимое кол-во запросов в секунду */
    @Column(name = "requests_per_second", nullable = false)
    private int requestsPerSecond;

    /** Пиковый всплеск — сколько запросов можно накопить */
    @Column(name = "burst_capacity", nullable = false)
    private int burstCapacity;

    /** Включено ли правило */
    @Column(nullable = false)
    private boolean enabled = true;

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

    public GatewayRoute getRoute() { return route; }
    public void setRoute(GatewayRoute route) { this.route = route; }

    public int getRequestsPerSecond() { return requestsPerSecond; }
    public void setRequestsPerSecond(int requestsPerSecond) { this.requestsPerSecond = requestsPerSecond; }

    public int getBurstCapacity() { return burstCapacity; }
    public void setBurstCapacity(int burstCapacity) { this.burstCapacity = burstCapacity; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public Instant getCreatedAt() { return createdAt; }
}
