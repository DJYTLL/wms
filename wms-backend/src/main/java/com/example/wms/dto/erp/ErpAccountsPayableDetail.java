package com.example.wms.dto.erp;

import com.example.wms.entity.erp.ErpAccountsPayable;

import java.util.List;

// ERP应付单详情
public record ErpAccountsPayableDetail(
    ErpAccountsPayable payable,
    String supplierName,
    List<ErpPaymentView> payments
) {
}
