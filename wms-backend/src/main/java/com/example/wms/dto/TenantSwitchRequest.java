package com.example.wms.dto;

import jakarta.validation.constraints.NotBlank;

/**

 * 租户用于接收接口请求参数。

 */
public record TenantSwitchRequest(
    /**
     * 表示租户编码。
     */
    @NotBlank String tenantCode
) {
}
