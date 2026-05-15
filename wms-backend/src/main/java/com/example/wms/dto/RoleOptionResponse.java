package com.example.wms.dto;

/**

 * 角色下拉选项用于返回接口响应数据。

 */
public record RoleOptionResponse(
    /**
     * 表示数据的主键 ID。
     */
    Long id,
    /**
     * 表示业务编码。
     */
    String code,
    /**
     * 表示名称。
     */
    String name
) {
}
