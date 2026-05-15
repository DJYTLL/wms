package com.example.wms.dto.erp;

import java.math.BigDecimal;

/**

 * ERP 供应商欠款用于返回视图展示数据。

 */
public class ErpSupplierDebtView {
    /**
     * 表示供应商 ID。
     */
    private Long supplierId;
    /**
     * 表示供应商名称。
     */
    private String supplierName;
    /**
     * 表示欠款总金额。
     */
    private BigDecimal totalDebt;

    public ErpSupplierDebtView() {
    }

    public ErpSupplierDebtView(Long supplierId, String supplierName, BigDecimal totalDebt) {
        this.supplierId = supplierId;
        this.supplierName = supplierName;
        this.totalDebt = totalDebt;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public BigDecimal getTotalDebt() {
        return totalDebt;
    }

    public void setTotalDebt(BigDecimal totalDebt) {
        this.totalDebt = totalDebt;
    }
}
