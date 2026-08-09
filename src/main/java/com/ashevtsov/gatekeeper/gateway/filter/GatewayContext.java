package com.ashevtsov.gatekeeper.gateway.filter;

import com.ashevtsov.gatekeeper.gateway.GatewayRoute;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Контекст одного proxy-запроса — несет в себе все что нужно фильтрам:
 * request/response, маршрут, атрибуты (кидай туда что хочешь) и время старта.
 * Создается в ProxyController на каждый входящий запрос.
 */
public class GatewayContext {

    /** Оригинальный HTTP-запрос от клиента */
    private final HttpServletRequest request;

    /** Ответ — фильтры могут дописать хедеры или даже заблокировать */
    private final HttpServletResponse response;

    /** Маршрут, по которому поедет запрос */
    private final GatewayRoute route;

    /** Произвольные атрибуты — фильтры могут кидать сюда свои данные */
    private final Map<String, Object> attributes;

    /** Когда запрос пришел — для метрик и логов */
    private final Instant startTime;

    public GatewayContext(HttpServletRequest request,
                          HttpServletResponse response,
                          GatewayRoute route) {
        this.request = request;
        this.response = response;
        this.route = route;
        this.attributes = new HashMap<>();
        this.startTime = Instant.now();
    }

    // --- геттеры ---

    public HttpServletRequest getRequest() {
        return request;
    }

    public HttpServletResponse getResponse() {
        return response;
    }

    public GatewayRoute getRoute() {
        return route;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public Instant getStartTime() {
        return startTime;
    }

    /**
     * Шорткат — достать атрибут с кастом, чтоб не писать (Type) get() каждый раз
     */
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) {
        return (T) attributes.get(key);
    }

    /**
     * Положить атрибут в контекст
     */
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }
}
