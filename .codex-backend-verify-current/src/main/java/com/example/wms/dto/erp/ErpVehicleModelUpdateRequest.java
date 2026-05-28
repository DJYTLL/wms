package com.example.wms.dto.erp;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**

 * ERP 车型用于接收更新操作的请求参数。

 */
public record ErpVehicleModelUpdateRequest(
    /**
     * 表示车系 ID。
     */
    @NotNull Long seriesId,
    /**
     * 表示业务编码。
     */
    @NotBlank String code,
    /**
     * 表示名称。
     */
    @NotBlank String name,
    /**
     * 表示年份起始。
     */
    Integer yearFrom,
    /**
     * 表示年份结束。
     */
    Integer yearTo,
    /**
     * 表示排量。
     */
    String displacement,
    /**
     * 表示发动机。
     */
    String engine,
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
