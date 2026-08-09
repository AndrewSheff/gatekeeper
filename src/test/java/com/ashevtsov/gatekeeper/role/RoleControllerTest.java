package com.ashevtsov.gatekeeper.role;

import com.ashevtsov.gatekeeper.AbstractIntegrationTest;
import com.ashevtsov.gatekeeper.role.dto.CreateRoleRequest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RoleControllerTest extends AbstractIntegrationTest {

    private static final UUID DEFAULT_TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID USERS_READ_PERM = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID ROUTES_READ_PERM = UUID.fromString("10000000-0000-0000-0000-000000000008");

    @Autowired
    TestRestTemplate restTemplate;

    @Test
    void createRole_andGetEffectivePermissions() {
        // Создаем роль VIEWER с users:read
        var viewerReq = new CreateRoleRequest("TEST_VIEWER", "Только просмотр", null,
                DEFAULT_TENANT_ID, Set.of(USERS_READ_PERM));
        var viewerResp = restTemplate.postForEntity("/api/v1/roles", viewerReq, JsonNode.class);
        assertEquals(HttpStatus.CREATED, viewerResp.getStatusCode());
        String viewerId = viewerResp.getBody().get("id").asText();

        // Создаем роль OPERATOR с parent=VIEWER и routes:read
        var operatorReq = new CreateRoleRequest("TEST_OPERATOR", "Оператор",
                UUID.fromString(viewerId), DEFAULT_TENANT_ID, Set.of(ROUTES_READ_PERM));
        var operatorResp = restTemplate.postForEntity("/api/v1/roles", operatorReq, JsonNode.class);
        assertEquals(HttpStatus.CREATED, operatorResp.getStatusCode());
        String operatorId = operatorResp.getBody().get("id").asText();

        // Effective permissions OPERATOR = свои + VIEWER
        var effectiveResp = restTemplate.getForEntity(
                "/api/v1/roles/" + operatorId + "/effective-permissions", JsonNode.class);
        assertEquals(HttpStatus.OK, effectiveResp.getStatusCode());
        assertTrue(effectiveResp.getBody().size() >= 2);
    }

    @Test
    void listPermissions() {
        var resp = restTemplate.getForEntity("/api/v1/roles/permissions", JsonNode.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertTrue(resp.getBody().get("totalElements").asInt() >= 17); // seed data
    }

    @Test
    void createRole_duplicateName_returns409() {
        var req = new CreateRoleRequest("UNIQUE_ROLE_DUP", null, null, DEFAULT_TENANT_ID, null);
        restTemplate.postForEntity("/api/v1/roles", req, JsonNode.class);

        var dupeResp = restTemplate.postForEntity("/api/v1/roles", req, JsonNode.class);
        assertEquals(HttpStatus.CONFLICT, dupeResp.getStatusCode());
    }
}
