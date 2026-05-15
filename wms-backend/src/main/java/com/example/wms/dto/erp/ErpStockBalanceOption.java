package com.example.wms.dto.erp;

import java.math.BigDecimal;

/**

 * ERP 库存结余用于返回下拉选项数据。

 */
public class ErpStockBalanceOption {
    /**
     * 表示仓库 ID。
     */
    private Long warehouseId;
    /**
     * 表示仓库名称。
     */
    private String warehouseName;
    /**
     * 表示库位 ID。
     */
    private Long locationId;
    /**
     * 表示库位名称。
     */
    private String locationName;
    /**
     * 表示数量OnHand。
     */
    private BigDecimal qtyOnHand;
    /**
     * 表示数量Available。
     */
    private BigDecimal qtyAvailable;
    /**
     * 表示数量锁定。
     */
    private BigDecimal qtyLocked;

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

    public BigDecimal getQtyOnHand() {
        return qtyOnHand;
    }

    public void setQtyOnHand(BigDecimal qtyOnHand) {
        this.qtyOnHand = qtyOnHand;
    }

    public BigDecimal getQtyAvailable() {
        return qtyAvailable;
    }

    public void setQtyAvailable(BigDecimal qtyAvailable) {
        this.qtyAvailable = qtyAvailable;
    }

    public BigDecimal getQtyLocked() {
        return qtyLocked;
    }

    public void setQtyLocked(BigDecimal qtyLocked) {
        this.qtyLocked = qtyLocked;
    }
}
