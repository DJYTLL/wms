package com.example.wms.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**

 * 租户用于接收新增操作的请求参数。

 */
public record TenantCreateRequest(
    /**
     * 表示业务编码。
     */
    @NotBlank String code,
    /**
     * 表示名称。
     */
    @NotBlank String name,
    /**
     * 表示管理员用户名。
     */
    String adminUsername,
    /**
     * 表示管理员Password。
     */
    String adminPassword,
    /**
     * 表示菜单 ID 列表。
     */
    List<Long> menuIds
) {
}
