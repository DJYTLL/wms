package com.example.wms.dto;

import jakarta.validation.constraints.NotBlank;

/**

 * 权限用于接收新增操作的请求参数。

 */
public record PermissionCreateRequest(
    /**
     * 表示业务编码。
     */
    @NotBlank String code,
    /**
     * 表示名称。
     */
    @NotBlank String name,
    /**
     * 表示描述。
     */
    String description,
    /**
     * 表示是否启用。
     */
    Boolean enabled
) {
}
