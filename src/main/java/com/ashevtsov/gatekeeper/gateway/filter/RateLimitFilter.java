package com.ashevtsov.gatekeeper.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Rate limit фильтр — контролирует частоту запросов от клиента.
 * Пока заглушка — реальная логика с Bucket4j будет в GK-15.
 * Позже будет дергать RateLimitService и резать запросы по правилам тенанта/маршрута.
 */
@Component
@Order(2)
public class RateLimitFilter implements GatewayFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    @Override
    public void doFilter(GatewayContext context, GatewayFilterChain chain) {
        // TODO: подключить Bucket4j + RateLimitService для реальной проверки лимитов
        log.trace("RateLimitFilter: пропускаем запрос (заглушка)");

        chain.doFilter(context);
    }
}
