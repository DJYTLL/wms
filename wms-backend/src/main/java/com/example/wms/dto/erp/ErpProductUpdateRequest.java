package com.example.wms.dto.erp;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.List;

// 更新商品请求（ERP进销存）
public record ErpProductUpdateRequest(
    @NotBlank String code,
    @NotBlank String name,
    String shortName,
    String spec,
    String model,
    Long categoryId,
    Long unitId,
    Long defaultWarehouseId,
    Long defaultLocationId,
    String barcode,
    String sku,
    String brand,
    String origin,
    BigDecimal weight,
    BigDecimal volume,
    BigDecimal costPrice,
    BigDecimal salePrice,
    BigDecimal taxRate,
    BigDecimal safetyStock,
    BigDecimal minStock,
    BigDecimal maxStock,
    Boolean batch,
    Integer shelfLifeDays,
    Boolean enabled,
    String extAttrs,
    String remark,
    List<ErpProductPriceItemRequest> priceItems
) {
}
