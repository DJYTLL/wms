package com.example.wms.dto.erp;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**

 * ERP 库位用于接收新增操作的请求参数。

 */
public record ErpLocationCreateRequest(
    /**
     * 表示仓库 ID。
     */
    @NotNull Long warehouseId,
    /**
     * 表示业务编码。
     */
    @NotBlank String code,
    /**
     * 表示名称。
     */
    String name,
    /**
     * 表示aisle。
     */
    String aisle,
    /**
     * 表示rack。
     */
    String rack,
    /**
     * 表示bin。
     */
    String bin,
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
