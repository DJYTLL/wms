package com.example.wms.dto;

import java.time.Instant;
import java.util.List;

/**
 * 角色列设置用于返回接口响应数据。
 */
public record RoleColumnSettingResponse(
    Long roleId,
    String pageKey,
    List<String> visibleColumns,
    String updatedBy,
    Instant updatedAt
) {
}
