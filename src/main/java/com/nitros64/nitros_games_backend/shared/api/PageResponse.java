package com.nitros64.nitros_games_backend.shared.api;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public record PageResponse<T>(
        List<T> content,
        PageableResponse pageable,
        boolean last,
        int totalPages,
        long totalElements,
        int size,
        int number,
        SortResponse sort,
        boolean first,
        int numberOfElements,
        boolean empty) {

    public static <S, T> PageResponse<T> from(Page<S> page, Function<S, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                PageableResponse.from(page.getPageable()),
                page.isLast(),
                page.getTotalPages(),
                page.getTotalElements(),
                page.getSize(),
                page.getNumber(),
                SortResponse.from(page.getSort()),
                page.isFirst(),
                page.getNumberOfElements(),
                page.isEmpty());
    }

    public record PageableResponse(
            int pageNumber,
            int pageSize,
            SortResponse sort,
            long offset,
            boolean paged,
            boolean unpaged) {

        private static PageableResponse from(Pageable pageable) {
            return new PageableResponse(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    SortResponse.from(pageable.getSort()),
                    pageable.getOffset(),
                    pageable.isPaged(),
                    pageable.isUnpaged());
        }
    }

    public record SortResponse(boolean empty, boolean sorted, boolean unsorted) {

        private static SortResponse from(Sort sort) {
            return new SortResponse(sort.isEmpty(), sort.isSorted(), sort.isUnsorted());
        }
    }
}
