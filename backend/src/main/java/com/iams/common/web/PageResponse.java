package com.iams.common.web;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * The mandatory pagination envelope for every list endpoint (API spec Section 1.5).
 * No list endpoint may return a bare array - collections are always wrapped.
 */
public record PageResponse<T>(List<T> data, PageMeta page, List<String> sort) {

    public record PageMeta(int number, int size, long totalElements, int totalPages) {
    }

    public static <T> PageResponse<T> from(Page<T> springPage, List<String> sort) {
        return new PageResponse<>(
                springPage.getContent(),
                new PageMeta(springPage.getNumber(), springPage.getSize(), springPage.getTotalElements(), springPage.getTotalPages()),
                sort
        );
    }
}
