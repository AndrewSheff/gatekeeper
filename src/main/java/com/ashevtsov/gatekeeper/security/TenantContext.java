package com.ashevtsov.gatekeeper.security;

import java.util.UUID;

/**
 * ThreadLocal для текущего тенанта — доступен в любом месте стека.
 * Заполняется из JWT claim `tenant_id`, очищается в фильтре.
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {}

    public static UUID getCurrentTenantId() {
        return CURRENT_TENANT.get();
    }

    public static void setCurrentTenantId(UUID tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
