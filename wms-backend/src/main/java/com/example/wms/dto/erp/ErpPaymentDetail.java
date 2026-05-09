package com.example.wms.dto.erp;

import com.example.wms.entity.erp.ErpPayment;

import java.util.List;

// ERP付款单详情
public record ErpPaymentDetail(
    ErpPayment payment,
    String supplierName,
    String orderNo,
    String payableNo,
    List<ErpPaymentPayableView> payables
) {
}
