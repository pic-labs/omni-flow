package aicreative.ai.controlplane.api.common;

import lombok.Data;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;


@Data
public class Pagination<T> {

    private List<T> content;
    private long total;
    private int page;
    private int size;
    private long totalPages;

    public static <T> Pagination<T> of(List<T> content, long total, int page, int size) {
        Pagination<T> result = new Pagination<>();
        result.setContent(content);
        result.setTotal(total);
        result.setPage(page);
        result.setSize(size);
        result.setTotalPages((total + size - 1) / size);
        return result;
    }

    public static <T> Pagination<T> empty(int page, int size) {
        return of(new ArrayList<>(), 0, page, size);
    }

    public <U> Pagination<U> map(Function<? super T, ? extends U> converter) {
        Assert.notNull(converter, "Converter must not be null");
        final List<U> nl = this.content.stream().map(converter).collect(Collectors.toList());
        return Pagination.of(nl, total, page, size);
    }
}