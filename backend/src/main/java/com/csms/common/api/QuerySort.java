package com.csms.common.api;
import com.csms.common.error.*;
import java.util.Map;
public final class QuerySort {
    private QuerySort() {}
    public static String order(String sortBy,String sortOrder,Map<String,String> allowed,String fallback) {
        if(sortOrder!=null && !sortOrder.equalsIgnoreCase("asc") && !sortOrder.equalsIgnoreCase("desc")) throw new BizException(ErrorCode.PARAM_ERROR,"排序方向只能是 asc 或 desc");
        if(sortBy==null || sortBy.isBlank()) return fallback;
        String column=allowed.get(sortBy);
        if(column==null) throw new BizException(ErrorCode.PARAM_ERROR,"不支持该排序字段");
        return column+("desc".equalsIgnoreCase(sortOrder)?" DESC":" ASC");
    }
}
