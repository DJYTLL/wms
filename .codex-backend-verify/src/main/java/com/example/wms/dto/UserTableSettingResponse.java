package com.example.wms.dto;

import java.time.Instant;
import java.util.Map;

// 用户表格配置响应
public record UserTableSettingResponse(
    String pageKey,
    Map<String, Object> config,
    String updatedBy,
    Instant updatedAt
) {
}
