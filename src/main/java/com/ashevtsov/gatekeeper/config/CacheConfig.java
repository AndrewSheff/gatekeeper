package com.ashevtsov.gatekeeper.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Конфиг Caffeine-кешей — in-process, быстрый, без network overhead.
 * TTL 5 минут — допустимая задержка для обновления конфигурации маршрутов и правил.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        var manager = new CaffeineCacheManager();
        manager.setCacheNames(List.of("routes", "ip-rules", "rate-configs"));
        manager.setCaffeine(caffeineCacheBuilder());
        return manager;
    }

    /**
     * Общий билдер — 5 минут TTL после записи, максимум 500 записей
     */
    private Caffeine<Object, Object> caffeineCacheBuilder() {
        return Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(500);
    }
}
