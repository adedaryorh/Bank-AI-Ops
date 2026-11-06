package com.bankops.workspace;

import com.bankops.common.PageResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

public final class WorkspaceQueries {
    private WorkspaceQueries() {}
    public static Instant instant(ResultSet row, String key) throws SQLException { return row.getTimestamp(key).toInstant(); }
    public static <T> PageResponse<T> page(List<T> rows, int page, int size, long count) {
        if(page<0 || size<1 || size>100 || page>100000) throw new IllegalArgumentException("Invalid pagination.");
        return new PageResponse<>(rows,page,size,count,(int)((count+size-1)/size));
    }
    public static int offset(int page, int size) {
        if(page<0 || page>100000 || size<1 || size>100) throw new IllegalArgumentException("Invalid pagination.");
        return page*size;
    }
}
