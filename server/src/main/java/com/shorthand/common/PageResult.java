package com.shorthand.common;

import lombok.Data;

import java.util.List;

@Data
public class PageResult {
    private List<?> records;
    private long total;
    private long pageNo;
    private long pageSize;

    public static PageResult of(List<?> records, long total, long pageNo, long pageSize) {
        PageResult p = new PageResult();
        p.records = records;
        p.total = total;
        p.pageNo = pageNo;
        p.pageSize = pageSize;
        return p;
    }
}
