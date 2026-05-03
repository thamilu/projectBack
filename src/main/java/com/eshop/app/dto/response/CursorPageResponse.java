package com.eshop.app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CursorPageResponse<T> {
    private List<T> data;
    private String nextCursor;
    private boolean hasMore;

    public static <T> CursorPageResponse<T> of(List<T> data, String nextCursor, boolean hasMore) {
        return CursorPageResponse.<T>builder()
            .data(data)
            .nextCursor(nextCursor)
            .hasMore(hasMore)
            .build();
    }
}
