package com.example.wms.dto.erp;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.List;

/**

 * ERP 商品用于接收更新操作的请求参数。

 */
public record ErpProductUpdateRequest(
    /**
     * 表示业务编码。
     */
    @NotBlank String code,
    /**
     * 表示名称。
     */
    @NotBlank String name,
    /**
     * 表示简称名称。
     */
    String shortName,
    /**
     * 表示商品类型。
     */
    String productType,
    /**
     * 表示spec。
     */
    String spec,
    /**
     * 表示车型。
     */
    String model,
    /**
     * 表示分类 ID。
     */
    Long categoryId,
    /**
     * 表示单位 ID。
     */
    Long unitId,
    /**
     * 表示默认仓库 ID。
     */
    Long defaultWarehouseId,
    /**
     * 表示默认库位 ID。
     */
    Long defaultLocationId,
    /**
     * 表示barcode。
     */
    String barcode,
    /**
     * 表示sku。
     */
    String sku,
    /**
     * 表示品牌。
     */
    String brand,
    /**
     * 表示origin。
     */
    String origin,
    /**
     * 表示weight。
     */
    BigDecimal weight,
    /**
     * 表示volume。
     */
    BigDecimal volume,
    /**
     * 表示成本价格。
     */
    BigDecimal costPrice,
    /**
     * 表示销售价格。
     */
    BigDecimal salePrice,
    /**
     * 表示税务Rate。
     */
    BigDecimal taxRate,
    /**
     * 表示safety库存。
     */
    BigDecimal safetyStock,
    /**
     * 表示min库存。
     */
    BigDecimal minStock,
    /**
     * 表示max库存。
     */
    BigDecimal maxStock,
    /**
     * 表示batch。
     */
    Boolean batch,
    /**
     * 表示shelfLifeDays。
     */
    Integer shelfLifeDays,
    /**
     * 表示是否启用。
     */
    Boolean enabled,
    /**
     * 表示extAttrs。
     */
    String extAttrs,
    /**
     * 表示备注说明。
     */
    String remark,
    /**
     * 表示价格Items。
     */
    List<ErpProductPriceItemRequest> priceItems
) {
}
