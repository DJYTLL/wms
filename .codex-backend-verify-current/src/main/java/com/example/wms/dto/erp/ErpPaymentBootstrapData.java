package com.example.wms.dto.erp;

import com.example.wms.entity.erp.ErpPaymentMethod;
import com.example.wms.entity.erp.ErpSettlementMethod;
import com.example.wms.entity.erp.ErpSupplier;

import java.util.List;

public record ErpPaymentBootstrapData(
    String nextPaymentNo,
    List<ErpSupplier> suppliers,
    List<ErpSettlementMethod> settlementMethods,
    List<ErpPaymentMethod> paymentMethods
) {
}
