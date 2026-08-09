package com.ashevtsov.gatekeeper.tenant;

import com.ashevtsov.gatekeeper.common.exception.ConflictException;
import com.ashevtsov.gatekeeper.common.exception.NotFoundException;
import com.ashevtsov.gatekeeper.tenant.dto.CreateTenantRequest;
import com.ashevtsov.gatekeeper.tenant.dto.TenantResponse;
import com.ashevtsov.gatekeeper.tenant.dto.UpdateTenantRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Бизнес-логика тенантов: CRUD + генерация API-ключа
 */
@Service
@Transactional(readOnly = true)
public class TenantService {

    private final TenantRepository tenantRepository;
    private final TenantMapper tenantMapper;

    public TenantService(TenantRepository tenantRepository, TenantMapper tenantMapper) {
        this.tenantRepository = tenantRepository;
        this.tenantMapper = tenantMapper;
    }

    @Transactional
    public TenantResponse create(CreateTenantRequest request) {
        if (tenantRepository.existsBySlug(request.slug())) {
            throw new ConflictException("Тенант со slug '%s' уже существует".formatted(request.slug()));
        }

        var tenant = new Tenant();
        tenant.setName(request.name());
        tenant.setSlug(request.slug());
        tenant.setApiKey(generateApiKey());
        tenant.setMaxRps(request.maxRps());

        return tenantMapper.toResponse(tenantRepository.save(tenant));
    }

    public TenantResponse getById(UUID id) {
        return tenantMapper.toResponse(findById(id));
    }

    public Page<TenantResponse> getAll(Pageable pageable) {
        return tenantRepository.findAll(pageable).map(tenantMapper::toResponse);
    }

    @Transactional
    public TenantResponse update(UUID id, UpdateTenantRequest request) {
        var tenant = findById(id);

        if (request.name() != null) {
            tenant.setName(request.name());
        }
        if (request.enabled() != null) {
            tenant.setEnabled(request.enabled());
        }
        if (request.maxRps() != null) {
            tenant.setMaxRps(request.maxRps());
        }

        return tenantMapper.toResponse(tenantRepository.save(tenant));
    }

    private Tenant findById(UUID id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Tenant", id));
    }

    /**
     * Генерим API-ключ с префиксом gk_live_ — сразу видно что за ключ
     */
    private String generateApiKey() {
        return "gk_live_" + UUID.randomUUID().toString().replace("-", "");
    }
}
