package com.ashevtsov.gatekeeper.tenant;

import com.ashevtsov.gatekeeper.AbstractIntegrationTest;
import com.ashevtsov.gatekeeper.tenant.dto.CreateTenantRequest;
import com.ashevtsov.gatekeeper.tenant.dto.UpdateTenantRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Интеграционный тест CRUD тенантов — от контроллера до БД
 */
class TenantControllerTest extends AbstractIntegrationTest {

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void crudFlow() throws Exception {
        // CREATE
        var createReq = new CreateTenantRequest("Test Corp", "test-corp", 50);
        var createResponse = restTemplate.postForEntity("/api/v1/tenants", createReq, JsonNode.class);

        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        var body = createResponse.getBody();
        assertNotNull(body);
        assertEquals("Test Corp", body.get("name").asText());
        assertEquals("test-corp", body.get("slug").asText());
        assertTrue(body.get("apiKey").asText().startsWith("gk_live_"));
        assertEquals(50, body.get("maxRps").asInt());

        String id = body.get("id").asText();

        // GET by ID
        var getResponse = restTemplate.getForEntity("/api/v1/tenants/" + id, JsonNode.class);
        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        assertEquals("Test Corp", getResponse.getBody().get("name").asText());

        // UPDATE
        var updateReq = new UpdateTenantRequest("Updated Corp", null, 200);
        var updateHeaders = new HttpHeaders();
        updateHeaders.setContentType(MediaType.APPLICATION_JSON);
        var updateEntity = new HttpEntity<>(objectMapper.writeValueAsString(updateReq), updateHeaders);
        var updateResponse = restTemplate.exchange("/api/v1/tenants/" + id, HttpMethod.PUT, updateEntity, JsonNode.class);

        assertEquals(HttpStatus.OK, updateResponse.getStatusCode());
        assertEquals("Updated Corp", updateResponse.getBody().get("name").asText());
        assertEquals(200, updateResponse.getBody().get("maxRps").asInt());

        // LIST
        var listResponse = restTemplate.getForEntity("/api/v1/tenants", JsonNode.class);
        assertEquals(HttpStatus.OK, listResponse.getStatusCode());
        assertTrue(listResponse.getBody().get("totalElements").asInt() >= 1);
    }

    @Test
    void create_duplicateSlug_returns409() {
        var req = new CreateTenantRequest("First", "dup-slug", 100);
        restTemplate.postForEntity("/api/v1/tenants", req, JsonNode.class);

        var dupeResponse = restTemplate.postForEntity("/api/v1/tenants", req, JsonNode.class);
        assertEquals(HttpStatus.CONFLICT, dupeResponse.getStatusCode());
    }

    @Test
    void create_invalidSlug_returns400() {
        var req = new CreateTenantRequest("Bad Slug", "INVALID SLUG!", 100);
        var response = restTemplate.postForEntity("/api/v1/tenants", req, JsonNode.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}
