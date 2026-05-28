package com.example.wms.dto.erp;

import jakarta.validation.constraints.NotBlank;

/**

 * ERP 分类用于接收更新操作的请求参数。

 */
public record ErpCategoryUpdateRequest(
    /**
     * 表示业务编码。
     */
    @NotBlank String code,
    /**
     * 表示名称。
     */
    @NotBlank String name,
    /**
     * 表示父级 ID。
     */
    Long parentId,
    /**
     * 表示level。
     */
    Integer level,
    /**
     * 表示排序编号。
     */
    Integer sortNo,
    /**
     * 表示是否启用。
     */
    Boolean enabled,
    /**
     * 表示备注说明。
     */
    String remark
) {
}
