package com.example.wms.dto.erp;

import jakarta.validation.constraints.NotNull;

/**

 * ERP 商品适配关系用于接收新增操作的请求参数。

 */
public record ErpProductFitmentCreateRequest(
    /**
     * 表示商品 ID。
     */
    @NotNull Long productId,
    /**
     * 表示车型 ID。
     */
    @NotNull Long modelId,
    /**
     * 表示备注说明。
     */
    String remark
) {
}
