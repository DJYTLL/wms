package com.example.wms.dto;

import jakarta.validation.constraints.NotNull;

/**

 * 租户用于接收更新操作的请求参数。

 */
public record TenantStatusUpdateRequest(
    /**
     * 表示是否启用。
     */
    @NotNull Boolean enabled
) {
}
