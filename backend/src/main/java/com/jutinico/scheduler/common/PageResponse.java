package com.jutinico.scheduler.common;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/** Una página de resultados. Formato propio y estable: no expone el {@code Page} de Spring en la API. */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
        return of(page, page.getContent().stream().map(mapper).toList());
    }

    public static <T> PageResponse<T> of(Page<?> page, List<T> content) {
        return new PageResponse<>(content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
