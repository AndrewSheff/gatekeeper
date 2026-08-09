package com.ashevtsov.gatekeeper.gateway.filter;

/**
 * Фильтр gateway-цепочки — каждый фильтр что-то делает с контекстом
 * (проверяет auth, режет rate limit, пишет логи) и передает дальше по цепочке.
 * Порядок выполнения определяется @Order на имплементациях.
 */
public interface GatewayFilter {

    /**
     * Обработка запроса — поколдовал с контекстом и передал дальше через chain.doFilter()
     */
    void doFilter(GatewayContext context, GatewayFilterChain chain);
}
