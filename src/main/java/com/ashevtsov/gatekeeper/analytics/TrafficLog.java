package com.ashevtsov.gatekeeper.analytics;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Запись трафик-лога — фиксируем каждый проксированный запрос.
 * Append-only — пишем и не трогаем. Используется для аналитики и дебага.
 * Не привязываем ManyToOne к Tenant/Route — это лог, тут UUID достаточно.
 */
@Entity
@Table(name = "traffic_logs")
public class TrafficLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "route_id")
    private UUID routeId;

    /** HTTP-метод (GET, POST, PUT, DELETE...) */
    @Column(length = 10)
    private String method;

    /** Путь запроса */
    @Column(length = 1000)
    private String path;

    /** HTTP-статус ответа */
    @Column(name = "status_code")
    private int statusCode;

    /** Время обработки запроса в миллисекундах */
    @Column(name = "latency_ms")
    private long latencyMs;

    /** IP-адрес клиента */
    @Column(name = "client_ip", length = 50)
    private String clientIp;

    /** Уникальный ID запроса — для трейсинга */
    @Column(name = "request_id", length = 100)
    private String requestId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    // --- геттеры и сеттеры ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public UUID getRouteId() { return routeId; }
    public void setRouteId(UUID routeId) { this.routeId = routeId; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public int getStatusCode() { return statusCode; }
    public void setStatusCode(int statusCode) { this.statusCode = statusCode; }

    public long getLatencyMs() { return latencyMs; }
    public void setLatencyMs(long latencyMs) { this.latencyMs = latencyMs; }

    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public Instant getCreatedAt() { return createdAt; }
}
