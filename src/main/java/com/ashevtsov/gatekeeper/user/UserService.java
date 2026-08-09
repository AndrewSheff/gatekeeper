package com.ashevtsov.gatekeeper.user;

import com.ashevtsov.gatekeeper.common.exception.ConflictException;
import com.ashevtsov.gatekeeper.common.exception.NotFoundException;
import com.ashevtsov.gatekeeper.role.Role;
import com.ashevtsov.gatekeeper.role.RoleRepository;
import com.ashevtsov.gatekeeper.tenant.Tenant;
import com.ashevtsov.gatekeeper.tenant.TenantRepository;
import com.ashevtsov.gatekeeper.user.dto.CreateUserRequest;
import com.ashevtsov.gatekeeper.user.dto.UpdateUserRequest;
import com.ashevtsov.gatekeeper.user.dto.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Управление пользователями: создание с BCrypt, поиск, soft delete
 */
@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       TenantRepository tenantRepository,
                       RoleRepository roleRepository,
                       UserMapper userMapper,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.roleRepository = roleRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        Tenant tenant = tenantRepository.findById(request.tenantId())
                .orElseThrow(() -> new NotFoundException("Tenant", request.tenantId()));

        if (userRepository.existsByUsernameAndTenantId(request.username(), tenant.getId())) {
            throw new ConflictException("Username '%s' уже занят".formatted(request.username()));
        }
        if (userRepository.existsByEmailAndTenantId(request.email(), tenant.getId())) {
            throw new ConflictException("Email '%s' уже используется".formatted(request.email()));
        }

        var user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setTenant(tenant);

        if (request.roleIds() != null && !request.roleIds().isEmpty()) {
            user.setRoles(loadRoles(request.roleIds()));
        }

        return userMapper.toResponse(userRepository.save(user));
    }

    public UserResponse getById(UUID id) {
        return userMapper.toResponse(findById(id));
    }

    public Page<UserResponse> getAll(UUID tenantId, String search, Pageable pageable) {
        Page<User> page;
        if (search != null && !search.isBlank()) {
            page = userRepository.searchByTenantId(tenantId, search, pageable);
        } else {
            page = userRepository.findByTenantId(tenantId, pageable);
        }
        return page.map(userMapper::toResponse);
    }

    @Transactional
    public UserResponse update(UUID id, UpdateUserRequest request) {
        var user = findById(id);

        if (request.email() != null) {
            if (userRepository.existsByEmailAndTenantId(request.email(), user.getTenant().getId())
                    && !request.email().equals(user.getEmail())) {
                throw new ConflictException("Email '%s' уже используется".formatted(request.email()));
            }
            user.setEmail(request.email());
        }
        if (request.firstName() != null) user.setFirstName(request.firstName());
        if (request.lastName() != null) user.setLastName(request.lastName());
        if (request.enabled() != null) user.setEnabled(request.enabled());
        if (request.roleIds() != null) {
            user.setRoles(loadRoles(request.roleIds()));
        }

        return userMapper.toResponse(userRepository.save(user));
    }

    private User findById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User", id));
    }

    private Set<Role> loadRoles(Set<UUID> roleIds) {
        var roles = new HashSet<>(roleRepository.findAllById(roleIds));
        if (roles.size() != roleIds.size()) {
            throw new NotFoundException("Некоторые роли не найдены");
        }
        return roles;
    }
}
