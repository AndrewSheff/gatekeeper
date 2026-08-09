package com.ashevtsov.gatekeeper.tenant;

import com.ashevtsov.gatekeeper.common.dto.PageResponse;
import com.ashevtsov.gatekeeper.tenant.dto.CreateTenantRequest;
import com.ashevtsov.gatekeeper.tenant.dto.TenantResponse;
import com.ashevtsov.gatekeeper.tenant.dto.UpdateTenantRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * CRUD тенантов — управление изолированными пространствами
 */
@RestController
@RequestMapping("/api/v1/tenants")
@Tag(name = "Tenants", description = "Управление тенантами")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создать тенант")
    public TenantResponse create(@Valid @RequestBody CreateTenantRequest request) {
        return tenantService.create(request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить тенант по ID")
    public TenantResponse getById(@PathVariable UUID id) {
        return tenantService.getById(id);
    }

    @GetMapping
    @Operation(summary = "Список тенантов")
    public PageResponse<TenantResponse> getAll(@PageableDefault(size = 20) Pageable pageable) {
        return PageResponse.of(tenantService.getAll(pageable));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить тенант")
    public TenantResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateTenantRequest request) {
        return tenantService.update(id, request);
    }
}
