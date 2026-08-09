package com.ashevtsov.gatekeeper.gateway;

import com.ashevtsov.gatekeeper.common.dto.PageResponse;
import com.ashevtsov.gatekeeper.gateway.dto.CreateRouteRequest;
import com.ashevtsov.gatekeeper.gateway.dto.RouteResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * CRUD маршрутов — создание, просмотр, обновление, удаление.
 * Трансформации заголовков идут вместе с маршрутом.
 */
@RestController
@RequestMapping("/api/v1/routes")
@Tag(name = "Routes", description = "Управление маршрутами gateway")
public class RouteController {

    private final RouteService routeService;

    public RouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создать маршрут")
    public RouteResponse create(@Valid @RequestBody CreateRouteRequest request) {
        return routeService.create(request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить маршрут по ID")
    public RouteResponse getById(@PathVariable UUID id) {
        return routeService.getById(id);
    }

    @GetMapping
    @Operation(summary = "Список маршрутов тенанта")
    public PageResponse<RouteResponse> getAll(
            @RequestParam UUID tenantId,
            @PageableDefault(size = 20) Pageable pageable) {
        return PageResponse.of(routeService.getAll(tenantId, pageable));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить маршрут")
    public RouteResponse update(@PathVariable UUID id, @Valid @RequestBody CreateRouteRequest request) {
        return routeService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Удалить маршрут")
    public void delete(@PathVariable UUID id) {
        routeService.delete(id);
    }
}
