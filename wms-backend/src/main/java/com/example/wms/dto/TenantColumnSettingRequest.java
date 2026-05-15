package com.example.wms.dto;

import java.util.List;

/**

 * 租户列设置用于接收接口请求参数。

 */
public record TenantColumnSettingRequest(
    /**
     * 表示可见列。
     */
    List<String> visibleColumns
) {
}
