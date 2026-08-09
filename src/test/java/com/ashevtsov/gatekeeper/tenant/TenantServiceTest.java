package com.ashevtsov.gatekeeper.tenant;

import com.ashevtsov.gatekeeper.common.exception.ConflictException;
import com.ashevtsov.gatekeeper.common.exception.NotFoundException;
import com.ashevtsov.gatekeeper.tenant.dto.CreateTenantRequest;
import com.ashevtsov.gatekeeper.tenant.dto.TenantResponse;
import com.ashevtsov.gatekeeper.tenant.dto.UpdateTenantRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    @Mock
    TenantRepository tenantRepository;

    TenantService tenantService;

    @BeforeEach
    void setUp() {
        // используем простой маппер вместо Spy — Java 25 не дружит с Mockito inline mocks на сгенерированных классах
        TenantMapper mapper = tenant -> new TenantResponse(
                tenant.getId(), tenant.getName(), tenant.getSlug(),
                tenant.getApiKey(), tenant.isEnabled(), tenant.getMaxRps(),
                tenant.getCreatedAt(), tenant.getUpdatedAt()
        );
        tenantService = new TenantService(tenantRepository, mapper);
    }

    @Test
    void create_success() {
        when(tenantRepository.existsBySlug("acme")).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> {
            Tenant t = inv.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });

        var response = tenantService.create(new CreateTenantRequest("Acme Corp", "acme", 200));

        assertNotNull(response.id());
        assertEquals("Acme Corp", response.name());
        assertEquals("acme", response.slug());
        assertTrue(response.apiKey().startsWith("gk_live_"));
        assertEquals(200, response.maxRps());
    }

    @Test
    void create_duplicateSlug_throwsConflict() {
        when(tenantRepository.existsBySlug("acme")).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> tenantService.create(new CreateTenantRequest("Acme", "acme", 100)));
    }

    @Test
    void getById_notFound_throwsNotFoundException() {
        var id = UUID.randomUUID();
        when(tenantRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> tenantService.getById(id));
    }

    @Test
    void update_changesOnlyProvidedFields() {
        var id = UUID.randomUUID();
        var tenant = new Tenant();
        tenant.setId(id);
        tenant.setName("Old Name");
        tenant.setSlug("old");
        tenant.setApiKey("gk_live_xxx");
        tenant.setEnabled(true);
        tenant.setMaxRps(100);

        when(tenantRepository.findById(id)).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = tenantService.update(id, new UpdateTenantRequest("New Name", null, null));

        assertEquals("New Name", response.name());
        assertTrue(response.enabled());
        assertEquals(100, response.maxRps());
    }
}
