package com.example.wms.dto.erp;

import com.example.wms.entity.erp.ErpReceipt;

import java.util.List;

// ERP收款单详情
public record ErpReceiptDetail(
    ErpReceipt receipt,
    String customerName,
    String orderNo,
    String receivableNo,
    List<ErpReceiptReceivableView> receivables
) {
}
