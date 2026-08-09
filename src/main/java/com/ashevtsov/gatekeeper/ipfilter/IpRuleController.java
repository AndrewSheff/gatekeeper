package com.ashevtsov.gatekeeper.ipfilter;

import com.ashevtsov.gatekeeper.common.exception.NotFoundException;
import com.ashevtsov.gatekeeper.ipfilter.dto.CreateIpRuleRequest;
import com.ashevtsov.gatekeeper.ipfilter.dto.IpRuleResponse;
import com.ashevtsov.gatekeeper.tenant.TenantRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * CRUD IP-правил — управление вайтлистами и блеклистами тенантов.
 * При создании/удалении сбрасываем кеш ip-rules, чтоб IpFilterService подтянул свежие данные.
 */
@RestController
@RequestMapping("/api/v1/ip-rules")
@Tag(name = "IP Rules", description = "Управление IP-правилами фильтрации")
@Transactional(readOnly = true)
public class IpRuleController {

    private final IpRuleRepository ipRuleRepository;
    private final TenantRepository tenantRepository;

    public IpRuleController(IpRuleRepository ipRuleRepository, TenantRepository tenantRepository) {
        this.ipRuleRepository = ipRuleRepository;
        this.tenantRepository = tenantRepository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создать IP-правило")
    @Transactional
    @CacheEvict(value = "ip-rules", key = "#request.tenantId()")
    public IpRuleResponse create(@Valid @RequestBody CreateIpRuleRequest request) {
        var tenant = tenantRepository.findById(request.tenantId())
                .orElseThrow(() -> new NotFoundException("Tenant", request.tenantId()));

        var rule = new IpRule();
        rule.setTenant(tenant);
        rule.setIpPattern(request.ipPattern());
        rule.setRuleType(request.ruleType());
        rule.setDescription(request.description());

        return toResponse(ipRuleRepository.save(rule));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить IP-правило по ID")
    public IpRuleResponse getById(@PathVariable UUID id) {
        return toResponse(findById(id));
    }

    @GetMapping
    @Operation(summary = "Список IP-правил тенанта")
    public List<IpRuleResponse> getByTenantId(@RequestParam UUID tenantId) {
        return ipRuleRepository.findByTenantId(tenantId).stream()
                .map(this::toResponse)
                .toList();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Удалить IP-правило")
    @Transactional
    @CacheEvict(value = "ip-rules", allEntries = true)
    public void delete(@PathVariable UUID id) {
        var rule = findById(id);
        ipRuleRepository.delete(rule);
    }

    /**
     * Маппим сущность в ответ — простая лямбда, MapStruct тут overkill
     */
    private IpRuleResponse toResponse(IpRule rule) {
        return new IpRuleResponse(
                rule.getId(),
                rule.getTenant().getId(),
                rule.getIpPattern(),
                rule.getRuleType(),
                rule.getDescription(),
                rule.getCreatedAt()
        );
    }

    private IpRule findById(UUID id) {
        return ipRuleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("IpRule", id));
    }
}
