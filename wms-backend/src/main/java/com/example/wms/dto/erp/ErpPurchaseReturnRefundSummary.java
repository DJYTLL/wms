package com.example.wms.dto.erp;

import java.math.BigDecimal;

// 原采购单退款资金摘要
public class ErpPurchaseReturnRefundSummary {
    private Long purchaseOrderId;
    private String purchaseOrderNo;
    private BigDecimal discountAmount;
    private BigDecimal paidCash;
    private BigDecimal refundedCash;
    private BigDecimal refundableCash;

    public Long getPurchaseOrderId() {
        return purchaseOrderId;
    }

    public void setPurchaseOrderId(Long purchaseOrderId) {
        this.purchaseOrderId = purchaseOrderId;
    }

    public String getPurchaseOrderNo() {
        return purchaseOrderNo;
    }

    public void setPurchaseOrderNo(String purchaseOrderNo) {
        this.purchaseOrderNo = purchaseOrderNo;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getPaidCash() {
        return paidCash;
    }

    public void setPaidCash(BigDecimal paidCash) {
        this.paidCash = paidCash;
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
