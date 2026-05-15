package com.example.wms.dto.erp;

import java.math.BigDecimal;

/**

 * ERP 库存预警用于返回视图展示数据。

 */
public class ErpStockWarningView {
    /**
     * 表示商品 ID。
     */
    private Long productId;
    /**
     * 表示商品编码。
     */
    private String productCode;
    /**
     * 表示商品名称。
     */
    private String productName;
    /**
     * 表示分类名称。
     */
    private String categoryName;
    /**
     * 表示单位名称。
     */
    private String unitName;
    /**
     * 表示默认仓库 ID。
     */
    private Long defaultWarehouseId;
    /**
     * 表示默认仓库名称。
     */
    private String defaultWarehouseName;
    /**
     * 表示默认库位 ID。
     */
    private Long defaultLocationId;
    /**
     * 表示默认库位名称。
     */
    private String defaultLocationName;
    /**
     * 表示合计数量。
     */
    private BigDecimal totalQty;
    /**
     * 表示min库存。
     */
    private BigDecimal minStock;
    /**
     * 表示max库存。
     */
    private BigDecimal maxStock;
    /**
     * 表示状态。
     */
    private String status;

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getUnitName() {
        return unitName;
    }

    public void setUnitName(String unitName) {
        this.unitName = unitName;
    }

    public Long getDefaultWarehouseId() {
        return defaultWarehouseId;
    }

    public void setDefaultWarehouseId(Long defaultWarehouseId) {
        this.defaultWarehouseId = defaultWarehouseId;
    }

    public String getDefaultWarehouseName() {
        return defaultWarehouseName;
    }

    public void setDefaultWarehouseName(String defaultWarehouseName) {
        this.defaultWarehouseName = defaultWarehouseName;
    }

    public Long getDefaultLocationId() {
        return defaultLocationId;
    }

    public void setDefaultLocationId(Long defaultLocationId) {
        this.defaultLocationId = defaultLocationId;
    }

    public String getDefaultLocationName() {
        return defaultLocationName;
    }

    public void setDefaultLocationName(String defaultLocationName) {
        this.defaultLocationName = defaultLocationName;
    }

    public BigDecimal getTotalQty() {
        return totalQty;
    }

    public void setTotalQty(BigDecimal totalQty) {
        this.totalQty = totalQty;
    }

    public BigDecimal getMinStock() {
        return minStock;
    }

    public void setMinStock(BigDecimal minStock) {
        this.minStock = minStock;
    }

    public BigDecimal getMaxStock() {
        return maxStock;
    }

    public void setMaxStock(BigDecimal maxStock) {
        this.maxStock = maxStock;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
