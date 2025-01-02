package dev.victor.streamingservice.model.base;

import java.util.List;

import org.springframework.data.domain.Page;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PageRespose<T> {

    private int page;
    private int limit;
    private long total;
    private int totalPages;
    private List<T> data;

    public PageRespose(int page, int limit, long total, int totalPages, List<T> data) {
        this.page = page;
        this.limit = limit;
        this.total = total;
        this.totalPages = totalPages;
        this.data = data;
    }

    public static <T> PageRespose<T> success(Page<T> pageData) {
        return new PageRespose<>(pageData.getNumber() + 1, pageData.getSize(), pageData.getTotalElements(),
                pageData.getTotalPages(), pageData.getContent());
    }
}
