package com.example.wms.dto.erp;

import com.example.wms.entity.erp.ErpPayment;

import java.util.List;

/**

 * ERP 付款单用于返回详情数据。

 */
public record ErpPaymentDetail(
    /**
     * 表示付款。
     */
    ErpPayment payment,
    /**
     * 表示供应商名称。
     */
    String supplierName,
    /**
     * 表示单据编号。
     */
    String orderNo,
    /**
     * 表示应付编号。
     */
    String payableNo,
    /**
     * 表示payables。
     */
    List<ErpPaymentPayableView> payables
) {
}
