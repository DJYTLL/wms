package com.example.wms.dto;

import jakarta.validation.constraints.NotBlank;

/**

 * 租户用于接收更新操作的请求参数。

 */
public record TenantUpdateRequest(
    /**
     * 表示名称。
     */
    @NotBlank String name
) {
}
