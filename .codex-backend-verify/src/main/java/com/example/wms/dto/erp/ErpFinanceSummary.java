package com.example.wms.dto.erp;

import java.math.BigDecimal;

/**

 * ERP 财务用于返回汇总统计数据。

 */
public class ErpFinanceSummary {
    /**
     * 表示客户Debt合计。
     */
    private BigDecimal customerDebtTotal;
    /**
     * 表示供应商Debt合计。
     */
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
