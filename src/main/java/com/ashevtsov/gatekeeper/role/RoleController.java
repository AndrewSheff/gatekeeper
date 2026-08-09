package com.ashevtsov.gatekeeper.role;

import com.ashevtsov.gatekeeper.common.dto.PageResponse;
import com.ashevtsov.gatekeeper.role.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.UUID;

/**
 * CRUD ролей + получение effective permissions с учетом иерархии
 */
@RestController
@RequestMapping("/api/v1/roles")
@Tag(name = "Roles", description = "Управление ролями и разрешениями")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создать роль")
    public RoleResponse create(@Valid @RequestBody CreateRoleRequest request) {
        return roleService.create(request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить роль по ID")
    public RoleResponse getById(@PathVariable UUID id) {
        return roleService.getById(id);
    }

    @GetMapping
    @Operation(summary = "Список ролей тенанта")
    public PageResponse<RoleResponse> getAll(
            @RequestParam UUID tenantId,
            @PageableDefault(size = 20) Pageable pageable) {
        return PageResponse.of(roleService.getAll(tenantId, pageable));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить роль")
    public RoleResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateRoleRequest request) {
        return roleService.update(id, request);
    }

    @GetMapping("/{id}/effective-permissions")
    @Operation(summary = "Effective permissions (с учетом иерархии)")
    public Set<PermissionResponse> getEffectivePermissions(@PathVariable UUID id) {
        return roleService.getEffectivePermissions(id);
    }

    @GetMapping("/permissions")
    @Operation(summary = "Все доступные permissions")
    public PageResponse<PermissionResponse> getAllPermissions(@PageableDefault(size = 100) Pageable pageable) {
        return PageResponse.of(roleService.getAllPermissions(pageable));
    }
}
