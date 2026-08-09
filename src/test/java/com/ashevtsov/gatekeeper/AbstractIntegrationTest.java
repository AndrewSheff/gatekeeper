package com.ashevtsov.gatekeeper;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Базовый класс для интеграционных тестов.
 * Использует H2 in-memory и отключает Redis.
 * В CI/CD с Docker — можно переключить на Testcontainers.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {
}
