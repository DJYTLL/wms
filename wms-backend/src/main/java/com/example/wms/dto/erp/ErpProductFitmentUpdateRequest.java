package com.example.wms.dto.erp;

/**

 * ERP 商品适配关系用于接收更新操作的请求参数。

 */
public record ErpProductFitmentUpdateRequest(
    /**
     * 表示备注说明。
     */
    String remark
) {
}
