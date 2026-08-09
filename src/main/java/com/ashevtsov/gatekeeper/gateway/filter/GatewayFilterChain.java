package com.ashevtsov.gatekeeper.gateway.filter;

import java.util.List;

/**
 * Цепочка фильтров с индексом — каждый вызов doFilter() двигает индекс вперед.
 * Когда фильтры кончились — просто возвращаемся, дальше ProxyController шлет запрос.
 * Классика Chain of Responsibility, ничего хитрого.
 */
public class GatewayFilterChain {

    /** Упорядоченный список фильтров */
    private final List<GatewayFilter> filters;

    /** Текущая позиция в цепочке */
    private int index = 0;

    public GatewayFilterChain(List<GatewayFilter> filters) {
        this.filters = filters;
    }

    /**
     * Дергаем следующий фильтр в цепочке.
     * Если фильтры закончились — молча выходим, значит все проверки пройдены.
     */
    public void doFilter(GatewayContext context) {
        if (index >= filters.size()) {
            return;
        }

        GatewayFilter currentFilter = filters.get(index);
        index++;
        currentFilter.doFilter(context, this);
    }
}
