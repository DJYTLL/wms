package com.example.wms.dto;

// 用户状态更新请求
public record UserStatusUpdateRequest(
    Boolean enabled,
    Boolean accountNonExpired,
    Boolean accountNonLocked,
    Boolean credentialsNonExpired
) {
}
