package com.ashevtsov.gatekeeper.ratelimit;

import com.ashevtsov.gatekeeper.ratelimit.dto.CreateRateLimitRequest;
import com.ashevtsov.gatekeeper.ratelimit.dto.RateLimitResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * CRUD rate limit правил — настройка ограничений частоты запросов.
 * Правила могут быть глобальными (на тенант) или привязаны к конкретному маршруту.
 */
@RestController
@RequestMapping("/api/v1/rate-limits")
@Tag(name = "Rate Limits", description = "Управление правилами rate limit")
public class RateLimitController {

    private final RateLimitService rateLimitService;

    public RateLimitController(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создать rate limit правило")
    public RateLimitResponse create(@Valid @RequestBody CreateRateLimitRequest request) {
        return rateLimitService.create(request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить rate limit правило по ID")
    public RateLimitResponse getById(@PathVariable UUID id) {
        return rateLimitService.getById(id);
    }

    @GetMapping
    @Operation(summary = "Список rate limit правил тенанта")
    public List<RateLimitResponse> getByTenantId(@RequestParam UUID tenantId) {
        return rateLimitService.getByTenantId(tenantId);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить rate limit правило")
    public RateLimitResponse update(@PathVariable UUID id, @Valid @RequestBody CreateRateLimitRequest request) {
        return rateLimitService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Удалить rate limit правило")
    public void delete(@PathVariable UUID id) {
        rateLimitService.delete(id);
    }
}
