package com.example.wms.dto.erp;

import java.math.BigDecimal;

// ERP 财务汇总
public class ErpFinanceSummary {
    private BigDecimal customerDebtTotal;
    private BigDecimal supplierDebtTotal;

    public ErpFinanceSummary() {
    }

    public ErpFinanceSummary(BigDecimal customerDebtTotal, BigDecimal supplierDebtTotal) {
        this.customerDebtTotal = customerDebtTotal;
        this.supplierDebtTotal = supplierDebtTotal;
    }

    public BigDecimal getCustomerDebtTotal() {
        return customerDebtTotal;
    }

    public void setCustomerDebtTotal(BigDecimal customerDebtTotal) {
        this.customerDebtTotal = customerDebtTotal;
    }

    public BigDecimal getSupplierDebtTotal() {
        return supplierDebtTotal;
    }

    public void setSupplierDebtTotal(BigDecimal supplierDebtTotal) {
        this.supplierDebtTotal = supplierDebtTotal;
    }
}
