package com.example.wms.dto.erp;

import jakarta.validation.constraints.NotEmpty;
import java.math.BigDecimal;
import java.util.List;

// Create assembly/disassembly order request
public record ErpAssemblyOrderCreateRequest(
    String orderNo,
    String orderType,
    String orderAt,
    Long finishedProductId,
    BigDecimal finishedQty,
    Long warehouseId,
    Long locationId,
    BigDecimal laborCost,
    @NotEmpty List<ErpAssemblyOrderItemRequest> items,
    String remark
) {
}
