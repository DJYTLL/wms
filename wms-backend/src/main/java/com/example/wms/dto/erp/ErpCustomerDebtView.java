package com.example.wms.dto.erp;

import java.math.BigDecimal;

// 客户欠款汇总
public class ErpCustomerDebtView {
    private Long customerId;
    private String customerName;
    private BigDecimal totalDebt;

    public ErpCustomerDebtView() {
    }

    public ErpCustomerDebtView(Long customerId, String customerName, BigDecimal totalDebt) {
        this.customerId = customerId;
        this.customerName = customerName;
        this.totalDebt = totalDebt;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public BigDecimal getTotalDebt() {
        return totalDebt;
    }

    public void setTotalDebt(BigDecimal totalDebt) {
        this.totalDebt = totalDebt;
    }
}
