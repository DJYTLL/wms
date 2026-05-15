package com.example.wms.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**

 * 用户角色用于接收更新操作的请求参数。

 */
public record UserRoleUpdateRequest(
    /**
     * 表示角色 ID 列表。
     */
    @NotEmpty List<Long> roleIds
) {
}
