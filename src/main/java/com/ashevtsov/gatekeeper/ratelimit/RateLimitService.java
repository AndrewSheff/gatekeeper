package com.ashevtsov.gatekeeper.ratelimit;

import com.ashevtsov.gatekeeper.common.exception.NotFoundException;
import com.ashevtsov.gatekeeper.gateway.GatewayRouteRepository;
import com.ashevtsov.gatekeeper.ratelimit.dto.CreateRateLimitRequest;
import com.ashevtsov.gatekeeper.ratelimit.dto.RateLimitResponse;
import com.ashevtsov.gatekeeper.tenant.TenantRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * CRUD-сервис rate limit правил — создание, получение, обновление, удаление.
 * При изменениях сбрасываем кеш rate-configs, чтоб RateLimitFilter подтянул свежее.
 */
@Service
@Transactional(readOnly = true)
public class RateLimitService {

    private final RateLimitRepository rateLimitRepository;
    private final TenantRepository tenantRepository;
    private final GatewayRouteRepository routeRepository;

    public RateLimitService(RateLimitRepository rateLimitRepository,
                            TenantRepository tenantRepository,
                            GatewayRouteRepository routeRepository) {
        this.rateLimitRepository = rateLimitRepository;
        this.tenantRepository = tenantRepository;
        this.routeRepository = routeRepository;
    }

    /**
     * Создаем правило — привязываем к тенанту и опционально к маршруту
     */
    @Transactional
    @CacheEvict(value = "rate-configs", allEntries = true)
    public RateLimitResponse create(CreateRateLimitRequest request) {
        var tenant = tenantRepository.findById(request.tenantId())
                .orElseThrow(() -> new NotFoundException("Tenant", request.tenantId()));

        var rule = new RateLimitRule();
        rule.setTenant(tenant);
        rule.setRequestsPerSecond(request.requestsPerSecond());
        rule.setBurstCapacity(request.burstCapacity());
        rule.setEnabled(request.enabled());

        // маршрут опционален — если задан, привязываем
        if (request.routeId() != null) {
            var route = routeRepository.findById(request.routeId())
                    .orElseThrow(() -> new NotFoundException("GatewayRoute", request.routeId()));
            rule.setRoute(route);
        }

        return toResponse(rateLimitRepository.save(rule));
    }

    public RateLimitResponse getById(UUID id) {
        return toResponse(findById(id));
    }

    public List<RateLimitResponse> getByTenantId(UUID tenantId) {
        return rateLimitRepository.findByTenantId(tenantId).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Обновляем правило — можно поменять лимиты и вкл/выкл
     */
    @Transactional
    @CacheEvict(value = "rate-configs", allEntries = true)
    public RateLimitResponse update(UUID id, CreateRateLimitRequest request) {
        var rule = findById(id);

        rule.setRequestsPerSecond(request.requestsPerSecond());
        rule.setBurstCapacity(request.burstCapacity());
        rule.setEnabled(request.enabled());

        if (request.routeId() != null) {
            var route = routeRepository.findById(request.routeId())
                    .orElseThrow(() -> new NotFoundException("GatewayRoute", request.routeId()));
            rule.setRoute(route);
        } else {
            rule.setRoute(null);
        }

        return toResponse(rateLimitRepository.save(rule));
    }

    /**
     * Удаляем правило — кеш сбрасывается автоматически
     */
    @Transactional
    @CacheEvict(value = "rate-configs", allEntries = true)
    public void delete(UUID id) {
        var rule = findById(id);
        rateLimitRepository.delete(rule);
    }

    /**
     * Маппим в ответ — лямбда вместо MapStruct, тут все просто
     */
    private RateLimitResponse toResponse(RateLimitRule rule) {
        return new RateLimitResponse(
                rule.getId(),
                rule.getTenant().getId(),
                rule.getRoute() != null ? rule.getRoute().getId() : null,
                rule.getRequestsPerSecond(),
                rule.getBurstCapacity(),
                rule.isEnabled(),
                rule.getCreatedAt()
        );
    }

    private RateLimitRule findById(UUID id) {
        return rateLimitRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("RateLimitRule", id));
    }
}
