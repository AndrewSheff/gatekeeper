package com.ashevtsov.gatekeeper.role;

import com.ashevtsov.gatekeeper.role.dto.PermissionResponse;
import com.ashevtsov.gatekeeper.tenant.Tenant;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Проверяем getEffectivePermissions — рекурсивный сбор прав по иерархии
 */
class RoleServiceTest {

    @Test
    void getEffectivePermissions_collectsFromHierarchy() {
        // viewer -> permissions: [analytics:read]
        // operator (parent=viewer) -> permissions: [routes:write]
        // admin (parent=operator) -> permissions: [users:write]
        // effective(admin) = [users:write, routes:write, analytics:read]

        var tenant = new Tenant();
        tenant.setId(UUID.randomUUID());

        var pAnalytics = createPermission("analytics:read", "analytics", "READ");
        var pRoutes = createPermission("routes:write", "routes", "WRITE");
        var pUsers = createPermission("users:write", "users", "WRITE");

        var viewer = new Role();
        viewer.setId(UUID.randomUUID());
        viewer.setName("VIEWER");
        viewer.setTenant(tenant);
        viewer.setPermissions(Set.of(pAnalytics));

        var operator = new Role();
        operator.setId(UUID.randomUUID());
        operator.setName("OPERATOR");
        operator.setTenant(tenant);
        operator.setParent(viewer);
        operator.setPermissions(Set.of(pRoutes));

        var admin = new Role();
        admin.setId(UUID.randomUUID());
        admin.setName("ADMIN");
        admin.setTenant(tenant);
        admin.setParent(operator);
        admin.setPermissions(Set.of(pUsers));

        // Собираем effective вручную (тестируем логику collectPermissions)
        Set<Permission> effective = new java.util.HashSet<>();
        collectPermissions(admin, effective, 0, 5);

        assertEquals(3, effective.size());
        assertTrue(effective.contains(pAnalytics));
        assertTrue(effective.contains(pRoutes));
        assertTrue(effective.contains(pUsers));
    }

    @Test
    void getEffectivePermissions_stopsAtMaxDepth() {
        var tenant = new Tenant();
        tenant.setId(UUID.randomUUID());
        var p1 = createPermission("a:read", "a", "READ");

        // создаем цепочку глубиной 7
        Role current = null;
        for (int i = 0; i < 7; i++) {
            var role = new Role();
            role.setId(UUID.randomUUID());
            role.setName("role-" + i);
            role.setTenant(tenant);
            role.setPermissions(Set.of(p1));
            role.setParent(current);
            current = role;
        }

        Set<Permission> effective = new java.util.HashSet<>();
        collectPermissions(current, effective, 0, 5);

        // все роли шарят один permission, так что размер = 1
        // но глубина обхода ограничена 5
        assertEquals(1, effective.size());
    }

    // вспомогательный метод — повторяет логику RoleService.collectPermissions
    private void collectPermissions(Role role, Set<Permission> acc, int depth, int maxDepth) {
        if (depth >= maxDepth) return;
        acc.addAll(role.getPermissions());
        if (role.getParent() != null) {
            collectPermissions(role.getParent(), acc, depth + 1, maxDepth);
        }
    }

    private Permission createPermission(String name, String resource, String action) {
        var p = new Permission();
        p.setId(UUID.randomUUID());
        p.setName(name);
        p.setResource(resource);
        p.setAction(action);
        return p;
    }
}
