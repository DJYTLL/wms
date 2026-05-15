package com.example.wms.dto.erp;

import java.math.BigDecimal;

// 原销售单退款资金摘要
public class ErpSaleReturnRefundSummary {
    private Long saleOrderId;
    private String saleOrderNo;
    private BigDecimal discountAmount;
    private BigDecimal collectedCash;
    private BigDecimal refundedCash;
    private BigDecimal refundableCash;

    public Long getSaleOrderId() {
        return saleOrderId;
    }

    public void setSaleOrderId(Long saleOrderId) {
        this.saleOrderId = saleOrderId;
    }

    public String getSaleOrderNo() {
        return saleOrderNo;
    }

    public void setSaleOrderNo(String saleOrderNo) {
        this.saleOrderNo = saleOrderNo;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getCollectedCash() {
        return collectedCash;
    }

    public void setCollectedCash(BigDecimal collectedCash) {
        this.collectedCash = collectedCash;
    }

    public BigDecimal getRefundedCash() {
        return refundedCash;
    }

    public void setRefundedCash(BigDecimal refundedCash) {
        this.refundedCash = refundedCash;
    }

    public BigDecimal getRefundableCash() {
        return refundableCash;
    }

    public void setRefundableCash(BigDecimal refundableCash) {
        this.refundableCash = refundableCash;
    }
}
