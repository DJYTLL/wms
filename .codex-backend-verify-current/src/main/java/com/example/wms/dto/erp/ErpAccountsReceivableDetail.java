package com.example.wms.dto.erp;

import com.example.wms.entity.erp.ErpAccountsReceivable;

/**

 * ERP 应收账款用于返回详情数据。

 */
public record ErpAccountsReceivableDetail(
    /**
     * 表示应收。
     */
    ErpAccountsReceivable receivable,
    /**
     * 表示客户名称。
     */
    String customerName,
    /**
     * 表示receipts。
     */
    java.util.List<ErpReceiptView> receipts
) {
}
