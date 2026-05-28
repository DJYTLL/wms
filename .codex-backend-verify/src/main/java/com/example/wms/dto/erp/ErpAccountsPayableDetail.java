package com.example.wms.dto.erp;

import com.example.wms.entity.erp.ErpAccountsPayable;

import java.util.List;

/**

 * ERP 应付账款用于返回详情数据。

 */
public record ErpAccountsPayableDetail(
    /**
     * 表示应付。
     */
    ErpAccountsPayable payable,
    /**
     * 表示供应商名称。
     */
    String supplierName,
    /**
     * 表示payments。
     */
    List<ErpPaymentView> payments
) {
}
