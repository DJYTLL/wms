package com.example.wms.dto.erp;

import com.example.wms.entity.erp.ErpCustomer;
import com.example.wms.entity.erp.ErpReceiptMethod;
import com.example.wms.entity.erp.ErpSettlementMethod;

import java.util.List;

public record ErpReceiptBootstrapData(
    String nextReceiptNo,
    List<ErpCustomer> customers,
    List<ErpSettlementMethod> settlementMethods,
    List<ErpReceiptMethod> receiptMethods
) {
}
