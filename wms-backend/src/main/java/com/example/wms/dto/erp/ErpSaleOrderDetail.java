package com.example.wms.dto.erp;

import com.example.wms.entity.erp.ErpSaleOrder;
import com.example.wms.entity.erp.ErpSaleOrderItem;

import java.math.BigDecimal;
import java.util.List;

/**

 * ERP 销售单用于返回详情数据。

 */
public record ErpSaleOrderDetail(
    /**
     * 表示主单信息。
     */
    ErpSaleOrder order,
    /**
     * 表示明细项列表。
     */
    List<ErpSaleOrderItem> items,
    /**
     * 表示客户Debt合计。
     */
    BigDecimal customerDebtTotal
) {
    public ErpSaleOrderDetail(ErpSaleOrder order, List<ErpSaleOrderItem> items) {
        this(order, items, BigDecimal.ZERO);
    }
}
