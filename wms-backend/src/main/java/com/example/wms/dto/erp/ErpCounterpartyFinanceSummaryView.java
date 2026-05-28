package com.example.wms.dto.erp;

import java.math.BigDecimal;

// 往来主体财务汇总视图
public class ErpCounterpartyFinanceSummaryView {
    private Long subjectId;
    private String subjectName;
    private BigDecimal receivableTotal;
    private BigDecimal payableTotal;
    private BigDecimal netAmount;
    private Integer customerCount;
    private Integer supplierCount;

    public ErpCounterpartyFinanceSummaryView() {
    }

    public ErpCounterpartyFinanceSummaryView(Long subjectId,
                                             String subjectName,
                                             BigDecimal receivableTotal,
                                             BigDecimal payableTotal,
                                             BigDecimal netAmount,
                                             Integer customerCount,
                                             Integer supplierCount) {
        this.subjectId = subjectId;
        this.subjectName = subjectName;
        this.receivableTotal = receivableTotal;
        this.payableTotal = payableTotal;
        this.netAmount = netAmount;
        this.customerCount = customerCount;
        this.supplierCount = supplierCount;
    }

    public Long getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Long subjectId) {
        this.subjectId = subjectId;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public BigDecimal getReceivableTotal() {
        return receivableTotal;
    }

    public void setReceivableTotal(BigDecimal receivableTotal) {
        this.receivableTotal = receivableTotal;
    }

    public BigDecimal getPayableTotal() {
        return payableTotal;
    }

    public void setPayableTotal(BigDecimal payableTotal) {
        this.payableTotal = payableTotal;
    }

    public BigDecimal getNetAmount() {
        return netAmount;
    }

    public void setNetAmount(BigDecimal netAmount) {
        this.netAmount = netAmount;
    }

    public Integer getCustomerCount() {
        return customerCount;
    }

    public void setCustomerCount(Integer customerCount) {
        this.customerCount = customerCount;
    }

    public Integer getSupplierCount() {
        return supplierCount;
    }

    public void setSupplierCount(Integer supplierCount) {
        this.supplierCount = supplierCount;
    }
}
