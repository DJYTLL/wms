package com.example.wms.dto.erp;

import java.time.Instant;
import java.util.List;

public record ErpAssemblySourceSaleOrderDetail(
    Long id,
    String orderNo,
    String status,
    Long customerId,
    String customerName,
    Instant orderAt,
    List<ErpAssemblySourceSaleOrderItem> items,
    List<com.example.wms.entity.erp.ErpAssemblyOrder> relatedAssemblies
) {
}
