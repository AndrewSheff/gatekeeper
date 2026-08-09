package com.ashevtsov.gatekeeper.gateway.filter;

import com.ashevtsov.gatekeeper.gateway.GatewayRoute;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Auth-фильтр — проверяет наличие и валидность Bearer-токена.
 * Если маршрут requireAuth=true, а токена нет — отбиваем 401.
 * Если маршрут задает requiredScopes — проверяем что токен содержит нужные скоупы.
 * Пока базовая реализация — парсим хедер, скоупы пока не валидируем по-настоящему.
 */
@Component
@Order(3)
public class AuthFilter implements GatewayFilter {

    private static final Logger log = LoggerFactory.getLogger(AuthFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public void doFilter(GatewayContext context, GatewayFilterChain chain) {
        GatewayRoute route = context.getRoute();

        // маршрут не требует авторизации — пропускаем
        if (!route.isRequireAuth()) {
            chain.doFilter(context);
            return;
        }

        String authHeader = context.getRequest().getHeader(AUTHORIZATION_HEADER);

        // нет хедера или не Bearer — 401
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            log.debug("AuthFilter: отсутствует Bearer-токен для маршрута '{}'", route.getName());
            sendError(context.getResponse(), HttpServletResponse.SC_UNAUTHORIZED, "Bearer token required");
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length()).trim();

        if (token.isEmpty()) {
            log.debug("AuthFilter: пустой Bearer-токен для маршрута '{}'", route.getName());
            sendError(context.getResponse(), HttpServletResponse.SC_UNAUTHORIZED, "Bearer token required");
            return;
        }

        // кладем токен в контекст — может пригодиться дальше по цепочке
        context.setAttribute("bearerToken", token);

        // проверяем requiredScopes если заданы
        String requiredScopes = route.getRequiredScopes();
        if (requiredScopes != null && !requiredScopes.isBlank()) {
            Set<String> required = Arrays.stream(requiredScopes.split("[,\\s]+"))
                    .filter(s -> !s.isBlank())
                    .collect(Collectors.toSet());

            // TODO: реальная валидация скоупов через JWT-декодирование
            // пока кладем required scopes в контекст — ProxyService может проверить позже
            context.setAttribute("requiredScopes", required);
            log.debug("AuthFilter: маршрут '{}' требует скоупы: {}", route.getName(), required);
        }

        chain.doFilter(context);
    }

    /**
     * Отправляем ошибку и коммитим response — дальше по цепочке не идем
     */
    private void sendError(HttpServletResponse response, int status, String message) {
        try {
            response.setStatus(status);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"%s\"}".formatted(message));
            response.getWriter().flush();
        } catch (IOException e) {
            log.error("Не удалось отправить ошибку авторизации", e);
        }
    }
}
