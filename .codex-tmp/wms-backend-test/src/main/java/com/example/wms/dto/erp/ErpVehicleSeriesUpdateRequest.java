package com.example.wms.dto.erp;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**

 * ERP 车系用于接收更新操作的请求参数。

 */
public record ErpVehicleSeriesUpdateRequest(
    /**
     * 表示品牌 ID。
     */
    @NotNull Long brandId,
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
