package com.ashevtsov.gatekeeper.gateway;

import jakarta.persistence.*;

import java.util.UUID;

/**
 * Трансформация заголовков для маршрута — добавить/удалить/перезаписать header.
 * type = ADD | REMOVE | SET, phase = REQUEST | RESPONSE.
 */
@Entity
@Table(name = "route_transformations")
public class RouteTransformation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private GatewayRoute route;

    @Column(nullable = false, length = 20)
    private String type;

    @Column(nullable = false, length = 20)
    private String phase;

    @Column(name = "header_name", nullable = false)
    private String headerName;

    @Column(name = "header_value", length = 1000)
    private String headerValue;

    // --- геттеры и сеттеры ---

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public GatewayRoute getRoute() { return route; }
    public void setRoute(GatewayRoute route) { this.route = route; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getPhase() { return phase; }
    public void setPhase(String phase) { this.phase = phase; }

    public String getHeaderName() { return headerName; }
    public void setHeaderName(String headerName) { this.headerName = headerName; }

    public String getHeaderValue() { return headerValue; }
    public void setHeaderValue(String headerValue) { this.headerValue = headerValue; }
}
