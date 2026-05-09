package com.example.wms.dto.erp;

import java.math.BigDecimal;

// 供应商欠款汇总
public class ErpSupplierDebtView {
    private Long supplierId;
    private String supplierName;
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
