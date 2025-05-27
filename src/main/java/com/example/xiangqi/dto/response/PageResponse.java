package com.example.xiangqi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {
    private int page;

    private int size;

    private long totalElements;

    private int totalPages;

    private List<T> content;

    public PageResponse(List<T> content, Pageable pageable, long totalElements) {
        this.page = pageable.getPageNumber();
        this.size = pageable.getPageSize();
        this.totalElements = totalElements;
        this.totalPages = (int) Math.ceil(totalElements / (double) size);
        this.content = content;
    }
}