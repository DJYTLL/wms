package com.example.wms.dto.erp;

import java.math.BigDecimal;

/**

 * ERP 客户欠款用于返回视图展示数据。

 */
public class ErpCustomerDebtView {
    /**
     * 表示客户 ID。
     */
    private Long customerId;
    /**
     * 表示客户名称。
     */
    private String customerName;
    /**
     * 表示欠款总金额。
     */
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
