package com.ashevtsov.gatekeeper.user;

import com.ashevtsov.gatekeeper.AbstractIntegrationTest;
import com.ashevtsov.gatekeeper.user.dto.CreateUserRequest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Интеграционные тесты User CRUD
 */
class UserControllerTest extends AbstractIntegrationTest {

    // ID из seed data (010-seed-data.xml)
    private static final UUID DEFAULT_TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID SUPER_ADMIN_ROLE_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");

    @Autowired
    TestRestTemplate restTemplate;

    @Test
    void createAndGetUser() {
        var request = new CreateUserRequest(
                "testuser", "test@example.com", "password123",
                "Test", "User", DEFAULT_TENANT_ID, Set.of(SUPER_ADMIN_ROLE_ID)
        );

        var createResponse = restTemplate.postForEntity("/api/v1/users", request, JsonNode.class);
        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());

        var body = createResponse.getBody();
        assertNotNull(body);
        assertEquals("testuser", body.get("username").asText());
        assertEquals("test@example.com", body.get("email").asText());
        assertTrue(body.get("enabled").asBoolean());
        assertFalse(body.has("passwordHash")); // пароль не должен утекать

        String id = body.get("id").asText();

        // GET by ID
        var getResponse = restTemplate.getForEntity("/api/v1/users/" + id, JsonNode.class);
        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        assertEquals("testuser", getResponse.getBody().get("username").asText());
    }

    @Test
    void create_duplicateUsername_returns409() {
        var request = new CreateUserRequest(
                "dupuser", "dup@example.com", "pass123456",
                null, null, DEFAULT_TENANT_ID, null
        );
        restTemplate.postForEntity("/api/v1/users", request, JsonNode.class);

        var dupeRequest = new CreateUserRequest(
                "dupuser", "dup2@example.com", "pass123456",
                null, null, DEFAULT_TENANT_ID, null
        );
        var dupeResponse = restTemplate.postForEntity("/api/v1/users", dupeRequest, JsonNode.class);
        assertEquals(HttpStatus.CONFLICT, dupeResponse.getStatusCode());
    }

    @Test
    void search_findsUserByUsername() {
        var request = new CreateUserRequest(
                "searchable", "searchable@test.com", "pass123456",
                null, null, DEFAULT_TENANT_ID, null
        );
        restTemplate.postForEntity("/api/v1/users", request, JsonNode.class);

        var searchResponse = restTemplate.getForEntity(
                "/api/v1/users?tenantId=" + DEFAULT_TENANT_ID + "&search=searchable",
                JsonNode.class
        );
        assertEquals(HttpStatus.OK, searchResponse.getStatusCode());
        assertTrue(searchResponse.getBody().get("totalElements").asInt() >= 1);
    }
}
