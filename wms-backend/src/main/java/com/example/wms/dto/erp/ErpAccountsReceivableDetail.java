package com.example.wms.dto.erp;

import com.example.wms.entity.erp.ErpAccountsReceivable;

// ERP应收单详情
public record ErpAccountsReceivableDetail(
    ErpAccountsReceivable receivable,
    String customerName,
    java.util.List<ErpReceiptView> receipts
) {
}
