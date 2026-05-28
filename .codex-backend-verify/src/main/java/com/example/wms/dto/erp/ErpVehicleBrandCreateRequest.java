package com.example.wms.dto.erp;

import jakarta.validation.constraints.NotBlank;

/**

 * ERP 车辆品牌用于接收新增操作的请求参数。

 */
public record ErpVehicleBrandCreateRequest(
    /**
     * 表示业务编码。
     */
    @NotBlank String code,
    /**
     * 表示名称。
     */
    @NotBlank String name,
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
