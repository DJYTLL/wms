package com.example.wms.dto;

import java.util.List;

// 分页响应结构
public record PageResponse<T>(
    long total,
    long page,
    long size,
    List<T> items
) {
}
