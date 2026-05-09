package com.example.wms.dto.erp;

import java.math.BigDecimal;

// 商品库存下拉明细（ERP进销存）
public class ErpStockBalanceOption {
    private Long warehouseId;
    private String warehouseName;
    private Long locationId;
    private String locationName;
    private BigDecimal qtyOnHand;
    private BigDecimal qtyAvailable;
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
