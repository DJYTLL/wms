package com.example.wms.dto.erp;

import jakarta.validation.constraints.NotBlank;

/**

 * ERP 仓库用于接收新增操作的请求参数。

 */
public record ErpWarehouseCreateRequest(
    /**
     * 表示业务编码。
     */
    @NotBlank String code,
    /**
     * 表示名称。
     */
    @NotBlank String name,
    /**
     * 表示地址。
     */
    String address,
    /**
     * 表示manager。
     */
    String manager,
    /**
     * 表示联系电话。
     */
    String phone,
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
