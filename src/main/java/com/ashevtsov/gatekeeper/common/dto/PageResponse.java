package com.ashevtsov.gatekeeper.common.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Обертка для пагинированных ответов.
 * Не привязана к Spring Page — клиент получает чистый контракт.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {
    /**
     * Создает PageResponse из Spring Page
     */
    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
