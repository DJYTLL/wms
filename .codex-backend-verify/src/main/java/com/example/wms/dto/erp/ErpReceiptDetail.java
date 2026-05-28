package com.example.wms.dto.erp;

import com.example.wms.entity.erp.ErpReceipt;

import java.util.List;

/**

 * ERP 收款单用于返回详情数据。

 */
public record ErpReceiptDetail(
    /**
     * 表示收款。
     */
    ErpReceipt receipt,
    /**
     * 表示客户名称。
     */
    String customerName,
    /**
     * 表示单据编号。
     */
    String orderNo,
    /**
     * 表示应收编号。
     */
    String receivableNo,
    /**
     * 表示receivables。
     */
    List<ErpReceiptReceivableView> receivables
) {
}
