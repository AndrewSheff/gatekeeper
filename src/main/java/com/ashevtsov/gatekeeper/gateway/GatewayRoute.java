package com.ashevtsov.gatekeeper.gateway;

import com.ashevtsov.gatekeeper.tenant.Tenant;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Маршрут gateway — определяет куда проксировать запрос.
 * predicatePath задает Ant-style паттерн, targetUrl — куда перенаправлять.
 * Привязан к тенанту, можно включать/выключать, требовать авторизацию.
 */
@Entity
@Table(name = "gateway_routes")
public class GatewayRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(nullable = false)
    private String name;

    @Column(name = "predicate_path", nullable = false, length = 500)
    private String predicatePath;

    @Column(name = "target_url", nullable = false, length = 1000)
    private String targetUrl;

    @Column(length = 100)
    private String methods;

    @Column(name = "strip_prefix", nullable = false)
    private int stripPrefix = 1;

    @Column(name = "order_priority", nullable = false)
    private int orderPriority = 0;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "require_auth", nullable = false)
    private boolean requireAuth = true;

    @Column(name = "required_scopes", length = 500)
    private String requiredScopes;

    @OneToMany(mappedBy = "route", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RouteTransformation> transformations = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    // --- геттеры и сеттеры ---

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Tenant getTenant() { return tenant; }
    public void setTenant(Tenant tenant) { this.tenant = tenant; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPredicatePath() { return predicatePath; }
    public void setPredicatePath(String predicatePath) { this.predicatePath = predicatePath; }

    public String getTargetUrl() { return targetUrl; }
    public void setTargetUrl(String targetUrl) { this.targetUrl = targetUrl; }

    public String getMethods() { return methods; }
    public void setMethods(String methods) { this.methods = methods; }

    public int getStripPrefix() { return stripPrefix; }
    public void setStripPrefix(int stripPrefix) { this.stripPrefix = stripPrefix; }

    public int getOrderPriority() { return orderPriority; }
    public void setOrderPriority(int orderPriority) { this.orderPriority = orderPriority; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public boolean isRequireAuth() { return requireAuth; }
    public void setRequireAuth(boolean requireAuth) { this.requireAuth = requireAuth; }

    public String getRequiredScopes() { return requiredScopes; }
    public void setRequiredScopes(String requiredScopes) { this.requiredScopes = requiredScopes; }

    public List<RouteTransformation> getTransformations() { return transformations; }
    public void setTransformations(List<RouteTransformation> transformations) { this.transformations = transformations; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
