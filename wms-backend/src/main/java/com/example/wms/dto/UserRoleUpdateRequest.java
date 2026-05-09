package com.example.wms.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

// 用户角色批量设置请求
public record UserRoleUpdateRequest(
    @NotEmpty List<Long> roleIds
) {
}
