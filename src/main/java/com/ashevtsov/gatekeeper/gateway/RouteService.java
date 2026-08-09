package com.ashevtsov.gatekeeper.gateway;

import com.ashevtsov.gatekeeper.common.exception.NotFoundException;
import com.ashevtsov.gatekeeper.gateway.dto.CreateRouteRequest;
import com.ashevtsov.gatekeeper.gateway.dto.RouteResponse;
import com.ashevtsov.gatekeeper.tenant.TenantRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * CRUD маршрутов + сброс кеша при изменениях.
 * Кеш routes сбрасывается при любой мутации — RouteResolver подтянет свежие данные.
 */
@Service
@Transactional(readOnly = true)
public class RouteService {

    private final GatewayRouteRepository routeRepository;
    private final TenantRepository tenantRepository;
    private final RouteMapper routeMapper;

    public RouteService(GatewayRouteRepository routeRepository,
                        TenantRepository tenantRepository,
                        RouteMapper routeMapper) {
        this.routeRepository = routeRepository;
        this.tenantRepository = tenantRepository;
        this.routeMapper = routeMapper;
    }

    /**
     * Создаем маршрут — вместе с трансформациями. Сбрасываем кеш.
     */
    @Transactional
    @CacheEvict(value = "routes", key = "#request.tenantId()")
    public RouteResponse create(CreateRouteRequest request) {
        var tenant = tenantRepository.findById(request.tenantId())
                .orElseThrow(() -> new NotFoundException("Tenant", request.tenantId()));

        var route = new GatewayRoute();
        route.setTenant(tenant);
        route.setName(request.name());
        route.setPredicatePath(request.predicatePath());
        route.setTargetUrl(request.targetUrl());
        route.setMethods(request.methods());
        route.setStripPrefix(request.stripPrefix());
        route.setOrderPriority(request.orderPriority());
        route.setEnabled(request.enabled());
        route.setRequireAuth(request.requireAuth());
        route.setRequiredScopes(request.requiredScopes());

        if (request.transformations() != null) {
            for (var t : request.transformations()) {
                var transformation = new RouteTransformation();
                transformation.setRoute(route);
                transformation.setType(t.type());
                transformation.setPhase(t.phase());
                transformation.setHeaderName(t.headerName());
                transformation.setHeaderValue(t.headerValue());
                route.getTransformations().add(transformation);
            }
        }

        return routeMapper.toResponse(routeRepository.save(route));
    }

    public RouteResponse getById(UUID id) {
        return routeMapper.toResponse(findById(id));
    }

    public Page<RouteResponse> getAll(UUID tenantId, Pageable pageable) {
        return routeRepository.findByTenantId(tenantId, pageable).map(routeMapper::toResponse);
    }

    /**
     * Обновляем маршрут — сбрасываем кеш по тенанту
     */
    @Transactional
    @CacheEvict(value = "routes", allEntries = true)
    public RouteResponse update(UUID id, CreateRouteRequest request) {
        var route = findById(id);

        route.setName(request.name());
        route.setPredicatePath(request.predicatePath());
        route.setTargetUrl(request.targetUrl());
        route.setMethods(request.methods());
        route.setStripPrefix(request.stripPrefix());
        route.setOrderPriority(request.orderPriority());
        route.setEnabled(request.enabled());
        route.setRequireAuth(request.requireAuth());
        route.setRequiredScopes(request.requiredScopes());

        // пересоздаем трансформации — orphanRemoval сам вычистит старые
        route.getTransformations().clear();
        if (request.transformations() != null) {
            for (var t : request.transformations()) {
                var transformation = new RouteTransformation();
                transformation.setRoute(route);
                transformation.setType(t.type());
                transformation.setPhase(t.phase());
                transformation.setHeaderName(t.headerName());
                transformation.setHeaderValue(t.headerValue());
                route.getTransformations().add(transformation);
            }
        }

        return routeMapper.toResponse(routeRepository.save(route));
    }

    /**
     * Удаляем маршрут — каскадно уберутся трансформации, кеш сбросим
     */
    @Transactional
    @CacheEvict(value = "routes", allEntries = true)
    public void delete(UUID id) {
        var route = findById(id);
        routeRepository.delete(route);
    }

    private GatewayRoute findById(UUID id) {
        return routeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("GatewayRoute", id));
    }
}
