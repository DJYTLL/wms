package com.example.wms.dto.erp;

import java.math.BigDecimal;
import java.util.List;

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
    private Long warehouseId;
    /**
     * 表示默认仓库名称。
     */
    private String warehouseName;
    /**
     * 表示默认库位 ID。
     */
    private Long locationId;
    /**
     * 表示默认库位名称。
     */
    private String locationName;
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
     * 表示安全库存。
     */
    private BigDecimal safetyStock;
    /**
     * 表示策略来源。
     */
    private String policySource;
    /**
     * 表示是否存在策略异常。
     */
    private Boolean hasPolicyAnomaly;
    /**
     * 表示异常类型。
     */
    private List<String> anomalyTypes;
    /**
     * 表示异常类型原始文本。
     */
    private String anomalyTypesText;
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

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getWarehouseName() {
        return warehouseName;
    }

    public void setWarehouseName(String warehouseName) {
        this.warehouseName = warehouseName;
    }

    public Long getLocationId() {
        return locationId;
    }

    public void setLocationId(Long locationId) {
        this.locationId = locationId;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
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

    public BigDecimal getSafetyStock() {
        return safetyStock;
    }

    public void setSafetyStock(BigDecimal safetyStock) {
        this.safetyStock = safetyStock;
    }

    public String getPolicySource() {
        return policySource;
    }

    public void setPolicySource(String policySource) {
        this.policySource = policySource;
    }

    public Boolean getHasPolicyAnomaly() {
        return hasPolicyAnomaly;
    }

    public void setHasPolicyAnomaly(Boolean hasPolicyAnomaly) {
        this.hasPolicyAnomaly = hasPolicyAnomaly;
    }

    public List<String> getAnomalyTypes() {
        return anomalyTypes;
    }

    public void setAnomalyTypes(List<String> anomalyTypes) {
        this.anomalyTypes = anomalyTypes;
    }

    public String getAnomalyTypesText() {
        return anomalyTypesText;
    }

    public void setAnomalyTypesText(String anomalyTypesText) {
        this.anomalyTypesText = anomalyTypesText;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
