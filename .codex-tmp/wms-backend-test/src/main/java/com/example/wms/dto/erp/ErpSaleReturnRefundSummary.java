package com.example.wms.dto.erp;

import java.math.BigDecimal;

/**

 * ERP 销售退货单用于返回汇总统计数据。

 */
public class ErpSaleReturnRefundSummary {
    /**
     * 表示销售Order ID。
     */
    private Long saleOrderId;
    /**
     * 表示销售Order编号。
     */
    private String saleOrderNo;
    /**
     * 表示优惠金额。
     */
    private BigDecimal discountAmount;
    /**
     * 表示collectedCash。
     */
    private BigDecimal collectedCash;
    /**
     * 表示refundedCash。
     */
    private BigDecimal refundedCash;
    /**
     * 表示refundableCash。
     */
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
