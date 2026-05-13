package com.example.wms.dto.erp;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

// Create assembly/disassembly template request
public record ErpAssemblyTemplateCreateRequest(
    @NotBlank String name,
    @NotBlank String orderType,
    @NotNull Long finishedProductId,
    @NotNull @Positive BigDecimal finishedQty,
    Long warehouseId,
    Long locationId,
    BigDecimal laborCost,
    @NotEmpty List<ErpAssemblyTemplateItemRequest> items,
    String remark
) {
}
