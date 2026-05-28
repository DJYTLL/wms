package com.example.wms.dto.erp;

import com.example.wms.entity.erp.ErpSaleReturn;
import com.example.wms.entity.erp.ErpSaleReturnItem;

import java.util.List;

/**

 * ERP 销售退货单用于返回详情数据。

 */
public record ErpSaleReturnDetail(
    /**
     * 表示主单信息。
     */
    ErpSaleReturn order,
    /**
     * 表示明细项列表。
     */
    List<ErpSaleReturnItem> items
) {
}
