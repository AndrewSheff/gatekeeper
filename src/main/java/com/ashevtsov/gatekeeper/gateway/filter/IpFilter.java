package com.ashevtsov.gatekeeper.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * IP-фильтр — первый рубеж обороны. Проверяет клиентский IP по правилам тенанта.
 * Если айпишник в блеклисте или не попал в вайтлист (когда тот существует) — отбиваем 403.
 * Пока заглушка — просто пропускаем дальше, IpFilterService появится в GK-15.
 */
@Component
@Order(1)
public class IpFilter implements GatewayFilter {

    private static final Logger log = LoggerFactory.getLogger(IpFilter.class);

    @Override
    public void doFilter(GatewayContext context, GatewayFilterChain chain) {
        // TODO: подключить IpFilterService из GK-15 для реальной проверки IP
        log.trace("IpFilter: пропускаем запрос (заглушка), ip={}", context.getRequest().getRemoteAddr());

        chain.doFilter(context);
    }
}
