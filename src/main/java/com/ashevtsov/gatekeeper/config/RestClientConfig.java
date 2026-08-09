package com.ashevtsov.gatekeeper.config;

import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.time.Duration;

/**
 * Конфигурация RestClient для проксирования — таймауты, чтоб не зависали запросы.
 * connect 5 секунд (хватит даже по VPN), read 30 секунд (для тяжелых отчетов и подобного).
 */
@Configuration
public class RestClientConfig {

    /** Таймаут на установление соединения */
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);

    /** Таймаут на чтение ответа */
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(30);

    /**
     * Кастомайзер для RestClient.Builder — вешаем таймауты на фабрику запросов.
     * Spring Boot подхватит его автоматически.
     */
    @Bean
    public RestClientCustomizer restClientCustomizer() {
        return builder -> {
            var requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
            requestFactory.setReadTimeout(READ_TIMEOUT);
            builder.requestFactory(requestFactory);
        };
    }
}
