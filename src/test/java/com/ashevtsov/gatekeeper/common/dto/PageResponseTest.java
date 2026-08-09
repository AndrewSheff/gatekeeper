package com.ashevtsov.gatekeeper.common.dto;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Проверяем маппинг Spring Page -> наш PageResponse
 */
class PageResponseTest {

    @Test
    void of_mapsSpringPageCorrectly() {
        var items = List.of("a", "b", "c");
        var springPage = new PageImpl<>(items, PageRequest.of(1, 10), 23);

        var response = PageResponse.of(springPage);

        assertEquals(List.of("a", "b", "c"), response.content());
        assertEquals(1, response.page());
        assertEquals(10, response.size());
        assertEquals(23, response.totalElements());
        assertEquals(3, response.totalPages());
        assertFalse(response.last());
    }

    @Test
    void of_lastPage_setsLastTrue() {
        var items = List.of("x");
        var springPage = new PageImpl<>(items, PageRequest.of(2, 10), 21);

        var response = PageResponse.of(springPage);

        assertTrue(response.last());
    }
}
