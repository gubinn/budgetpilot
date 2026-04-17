package uk.gubin.budgetpilot.common;

import lombok.Data;

import java.util.List;

@Data
public class PageResult<T> {
    private List<T> items;
    private long total;
    private int page;
    private int size;
    private int totalPages;

    public static <T> PageResult<T> of(List<T> items, long total, int page, int size) {
        PageResult<T> result = new PageResult<>();
        result.setItems(items);
        result.setTotal(total);
        result.setPage(page);
        result.setSize(size);
        result.setTotalPages((int) Math.ceil((double) total / size));
        return result;
    }
}
