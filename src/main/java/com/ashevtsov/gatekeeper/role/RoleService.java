package com.ashevtsov.gatekeeper.role;

import com.ashevtsov.gatekeeper.common.exception.ConflictException;
import com.ashevtsov.gatekeeper.common.exception.NotFoundException;
import com.ashevtsov.gatekeeper.role.dto.CreateRoleRequest;
import com.ashevtsov.gatekeeper.role.dto.PermissionResponse;
import com.ashevtsov.gatekeeper.role.dto.RoleResponse;
import com.ashevtsov.gatekeeper.role.dto.UpdateRoleRequest;
import com.ashevtsov.gatekeeper.tenant.TenantRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Управление ролями: CRUD + иерархия permissions.
 * getEffectivePermissions рекурсивно собирает права от parent-ов (maxDepth=5).
 */
@Service
@Transactional(readOnly = true)
public class RoleService {

    private static final int MAX_HIERARCHY_DEPTH = 5;

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final TenantRepository tenantRepository;
    private final RoleMapper roleMapper;

    public RoleService(RoleRepository roleRepository,
                       PermissionRepository permissionRepository,
                       TenantRepository tenantRepository,
                       RoleMapper roleMapper) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.tenantRepository = tenantRepository;
        this.roleMapper = roleMapper;
    }

    @Transactional
    public RoleResponse create(CreateRoleRequest request) {
        var tenant = tenantRepository.findById(request.tenantId())
                .orElseThrow(() -> new NotFoundException("Tenant", request.tenantId()));

        if (roleRepository.existsByNameAndTenantId(request.name(), tenant.getId())) {
            throw new ConflictException("Роль '%s' уже существует в этом тенанте".formatted(request.name()));
        }

        var role = new Role();
        role.setName(request.name());
        role.setDescription(request.description());
        role.setTenant(tenant);

        if (request.parentId() != null) {
            var parent = roleRepository.findById(request.parentId())
                    .orElseThrow(() -> new NotFoundException("Role", request.parentId()));
            role.setParent(parent);
        }

        if (request.permissionIds() != null && !request.permissionIds().isEmpty()) {
            role.setPermissions(loadPermissions(request.permissionIds()));
        }

        return roleMapper.toResponse(roleRepository.save(role));
    }

    public RoleResponse getById(UUID id) {
        return roleMapper.toResponse(findById(id));
    }

    public Page<RoleResponse> getAll(UUID tenantId, Pageable pageable) {
        return roleRepository.findByTenantId(tenantId, pageable).map(roleMapper::toResponse);
    }

    @Transactional
    public RoleResponse update(UUID id, UpdateRoleRequest request) {
        var role = findById(id);

        if (request.name() != null) {
            if (roleRepository.existsByNameAndTenantId(request.name(), role.getTenant().getId())
                    && !request.name().equals(role.getName())) {
                throw new ConflictException("Роль '%s' уже существует".formatted(request.name()));
            }
            role.setName(request.name());
        }
        if (request.description() != null) role.setDescription(request.description());
        if (request.parentId() != null) {
            var parent = roleRepository.findById(request.parentId())
                    .orElseThrow(() -> new NotFoundException("Role", request.parentId()));
            role.setParent(parent);
        }
        if (request.permissionIds() != null) {
            role.setPermissions(loadPermissions(request.permissionIds()));
        }

        return roleMapper.toResponse(roleRepository.save(role));
    }

    /**
     * Собираем все permissions роли + рекурсивно от parent-ов.
     * Защита от циклов через maxDepth.
     */
    public Set<PermissionResponse> getEffectivePermissions(UUID roleId) {
        var role = findById(roleId);
        Set<Permission> effective = new HashSet<>();
        collectPermissions(role, effective, 0);
        return effective.stream()
                .map(roleMapper::toPermissionResponse)
                .collect(Collectors.toSet());
    }

    private void collectPermissions(Role role, Set<Permission> accumulated, int depth) {
        if (depth >= MAX_HIERARCHY_DEPTH) return;

        accumulated.addAll(role.getPermissions());

        if (role.getParent() != null) {
            collectPermissions(role.getParent(), accumulated, depth + 1);
        }
    }

    public Page<PermissionResponse> getAllPermissions(Pageable pageable) {
        return permissionRepository.findAll(pageable).map(roleMapper::toPermissionResponse);
    }

    private Role findById(UUID id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Role", id));
    }

    private Set<Permission> loadPermissions(Set<UUID> ids) {
        var permissions = new HashSet<>(permissionRepository.findAllById(ids));
        if (permissions.size() != ids.size()) {
            throw new NotFoundException("Некоторые permissions не найдены");
        }
        return permissions;
    }
}
