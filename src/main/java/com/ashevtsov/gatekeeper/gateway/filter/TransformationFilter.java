package com.ashevtsov.gatekeeper.gateway.filter;

import com.ashevtsov.gatekeeper.gateway.RouteTransformation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Фильтр трансформации заголовков — применяет правила из route.transformations.
 * Поддерживает типы: ADD (добавить хедер), SET (перезаписать), REMOVE (удалить).
 * Работает только с phase=REQUEST — трансформации ответа пока не реализованы.
 * Оборачивает оригинальный request в враппер с подправленными заголовками.
 */
@Component
@Order(4)
public class TransformationFilter implements GatewayFilter {

    private static final Logger log = LoggerFactory.getLogger(TransformationFilter.class);

    @Override
    public void doFilter(GatewayContext context, GatewayFilterChain chain) {
        var transformations = context.getRoute().getTransformations();

        if (transformations == null || transformations.isEmpty()) {
            chain.doFilter(context);
            return;
        }

        // собираем только REQUEST-фазу
        var requestTransformations = transformations.stream()
                .filter(t -> "REQUEST".equalsIgnoreCase(t.getPhase()))
                .toList();

        if (requestTransformations.isEmpty()) {
            chain.doFilter(context);
            return;
        }

        // собираем модифицированные хедеры на базе оригинального запроса
        Map<String, List<String>> modifiedHeaders = buildModifiedHeaders(
                context.getRequest(), requestTransformations);

        // оборачиваем request в враппер с новыми хедерами
        HttpServletRequest wrappedRequest = new HeaderModifyingRequestWrapper(
                context.getRequest(), modifiedHeaders);

        // кладем обернутый запрос в атрибуты — ProxyService должен использовать его
        context.setAttribute("wrappedRequest", wrappedRequest);

        log.debug("TransformationFilter: применено {} трансформаций для маршрута '{}'",
                requestTransformations.size(), context.getRoute().getName());

        chain.doFilter(context);
    }

    /**
     * Строим новую карту хедеров, применяя трансформации последовательно
     */
    private Map<String, List<String>> buildModifiedHeaders(
            HttpServletRequest request, List<RouteTransformation> transformations) {

        // копируем оригинальные хедеры
        Map<String, List<String>> headers = new LinkedHashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            List<String> values = new ArrayList<>(Collections.list(request.getHeaders(name)));
            headers.put(name.toLowerCase(), values);
        }

        // применяем трансформации
        for (var t : transformations) {
            String headerName = t.getHeaderName().toLowerCase();
            String type = t.getType().toUpperCase();

            switch (type) {
                case "ADD" -> {
                    headers.computeIfAbsent(headerName, k -> new ArrayList<>())
                            .add(t.getHeaderValue());
                    log.trace("ADD header: {}={}", headerName, t.getHeaderValue());
                }
                case "SET" -> {
                    headers.put(headerName, new ArrayList<>(List.of(t.getHeaderValue())));
                    log.trace("SET header: {}={}", headerName, t.getHeaderValue());
                }
                case "REMOVE" -> {
                    headers.remove(headerName);
                    log.trace("REMOVE header: {}", headerName);
                }
                default -> log.warn("Неизвестный тип трансформации: {}", type);
            }
        }

        return headers;
    }

    /**
     * Враппер для HttpServletRequest — подменяет хедеры на модифицированные.
     * Все остальное делегируется оригинальному запросу.
     */
    private static class HeaderModifyingRequestWrapper extends HttpServletRequestWrapper {

        private final Map<String, List<String>> modifiedHeaders;

        HeaderModifyingRequestWrapper(HttpServletRequest request, Map<String, List<String>> modifiedHeaders) {
            super(request);
            this.modifiedHeaders = modifiedHeaders;
        }

        @Override
        public String getHeader(String name) {
            List<String> values = modifiedHeaders.get(name.toLowerCase());
            return (values != null && !values.isEmpty()) ? values.getFirst() : null;
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            List<String> values = modifiedHeaders.get(name.toLowerCase());
            return Collections.enumeration(values != null ? values : Collections.emptyList());
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            return Collections.enumeration(modifiedHeaders.keySet());
        }
    }
}
