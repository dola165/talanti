package ge.dola.talanti.util;

import java.util.List;

// Replaces Spring Data's Page<T>
public record PageResult<T>(
        List<T> content,
        int pageNumber,
        int pageSize,
        long totalElements
) {
    public int getTotalPages() {
        return pageSize == 0 ? 1 : (int) Math.ceil((double) totalElements / pageSize);
    }
}