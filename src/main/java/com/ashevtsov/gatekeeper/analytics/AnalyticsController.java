package com.ashevtsov.gatekeeper.analytics;

import com.ashevtsov.gatekeeper.analytics.dto.TrafficOverview;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Контроллер аналитики — обзор трафика для дашборда.
 * По умолчанию показывает данные за последние 24 часа.
 */
@RestController
@RequestMapping("/api/v1/analytics")
@Tag(name = "Analytics", description = "Аналитика трафика")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    /**
     * Обзор трафика — основные метрики за период.
     * from/to опциональны — по умолчанию берем последние 24 часа.
     */
    @GetMapping("/overview")
    @Operation(summary = "Обзор трафика за период")
    public TrafficOverview getOverview(
            @RequestParam UUID tenantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {

        // дефолт — последние 24 часа
        if (to == null) {
            to = Instant.now();
        }
        if (from == null) {
            from = to.minus(24, ChronoUnit.HOURS);
        }

        return analyticsService.getOverview(tenantId, from, to);
    }
}
