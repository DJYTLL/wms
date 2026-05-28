package com.example.wms.dto.erp;

import java.math.BigDecimal;

/**

 * ERP 采购退货单用于返回汇总统计数据。

 */
public class ErpPurchaseReturnRefundSummary {
    /**
     * 表示purchaseOrder ID。
     */
    private Long purchaseOrderId;
    /**
     * 表示purchaseOrder编号。
     */
    private String purchaseOrderNo;
    /**
     * 表示优惠金额。
     */
    private BigDecimal discountAmount;
    /**
     * 表示paidCash。
     */
    private BigDecimal paidCash;
    /**
     * 表示refundedCash。
     */
    private BigDecimal refundedCash;
    /**
     * 表示refundableCash。
     */
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
