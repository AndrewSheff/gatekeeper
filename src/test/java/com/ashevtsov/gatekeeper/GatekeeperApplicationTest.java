package com.ashevtsov.gatekeeper;

import org.junit.jupiter.api.Test;

/**
 * Smoke-тест: проверяем что Spring-контекст поднимается без ошибок
 */
class GatekeeperApplicationTest extends AbstractIntegrationTest {

    @Test
    void contextLoads() {
        // контекст поднялся — все зависимости на месте
    }
}
