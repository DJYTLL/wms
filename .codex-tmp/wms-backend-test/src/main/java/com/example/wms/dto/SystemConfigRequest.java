package com.example.wms.dto;

/**

 * 系统配置用于接收接口请求参数。

 */
public record SystemConfigRequest(
    /**
     * 表示值。
     */
    String value,
    /**
     * 表示值类型。
     */
    String valueType,
    /**
     * 表示描述。
     */
    String description,
    /**
     * 表示是否公开。
     */
    Boolean isPublic
) {
}
