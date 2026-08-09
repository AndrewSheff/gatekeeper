package com.ashevtsov.gatekeeper.common.exception;

import com.ashevtsov.gatekeeper.common.dto.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Проверяем что каждый тип exception маппится в правильный HTTP-статус
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/test");

    @Test
    void notFound_returns404() {
        var response = handler.handleNotFound(new NotFoundException("User", "123"), request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(404, body.status());
        assertTrue(body.message().contains("123"));
        assertEquals("/api/v1/test", body.path());
    }

    @Test
    void conflict_returns409() {
        var response = handler.handleConflict(new ConflictException("Username уже занят"), request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().status());
        assertEquals("Username уже занят", response.getBody().message());
    }

    @Test
    void illegalArgument_returns400() {
        var response = handler.handleIllegalArgument(new IllegalArgumentException("bad param"), request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().status());
    }

    @Test
    void genericException_returns500() {
        var response = handler.handleGeneral(new RuntimeException("boom"), request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(500, response.getBody().status());
        assertEquals("Внутренняя ошибка сервера", response.getBody().message());
    }
}
