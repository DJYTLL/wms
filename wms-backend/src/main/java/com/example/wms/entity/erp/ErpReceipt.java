package com.example.wms.entity.erp;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.wms.entity.base.TenantAuditableSoftDeleteEntity;

import java.math.BigDecimal;
import java.time.Instant;

// ERP收款单实体
@TableName("erp_receipt")
public class ErpReceipt extends TenantAuditableSoftDeleteEntity {
    @TableField("receivable_id")
    private Long receivableId;

    @TableField("sale_order_id")
    private Long saleOrderId;

    @TableField("receipt_no")
    private String receiptNo;

    @TableField("customer_id")
    private Long customerId;

    @TableField("amount")
    private BigDecimal amount;

    @TableField("discount_amount")
    private BigDecimal discountAmount;

    @TableField("settlement_method")
    private String settlementMethod;

    @TableField("status")
    private String status;

    @TableField("received_at")
    private Instant receivedAt;

    @TableField("remark")
    private String remark;

    @TableField("red_flush_source_type")
    private String redFlushSourceType;

    @TableField("red_flush_source_id")
    private Long redFlushSourceId;

    @TableField("print_count")
    private Integer printCount;

    @TableField("last_printed_at")
    private Instant lastPrintedAt;

    public Long getReceivableId() {
        return receivableId;
    }

    public void setReceivableId(Long receivableId) {
        this.receivableId = receivableId;
    }

    public Long getSaleOrderId() {
        return saleOrderId;
    }

    public void setSaleOrderId(Long saleOrderId) {
        this.saleOrderId = saleOrderId;
    }

    public String getReceiptNo() {
        return receiptNo;
    }

    public void setReceiptNo(String receiptNo) {
        this.receiptNo = receiptNo;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public String getSettlementMethod() {
        return settlementMethod;
    }

    public void setSettlementMethod(String settlementMethod) {
        this.settlementMethod = settlementMethod;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(Instant receivedAt) {
        this.receivedAt = receivedAt;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getRedFlushSourceType() {
        return redFlushSourceType;
    }

    public void setRedFlushSourceType(String redFlushSourceType) {
        this.redFlushSourceType = redFlushSourceType;
    }

    public Long getRedFlushSourceId() {
        return redFlushSourceId;
    }

    public void setRedFlushSourceId(Long redFlushSourceId) {
        this.redFlushSourceId = redFlushSourceId;
    }

    public Integer getPrintCount() {
        return printCount;
    }

    public void setPrintCount(Integer printCount) {
        this.printCount = printCount;
    }

    public Instant getLastPrintedAt() {
        return lastPrintedAt;
    }

    public void setLastPrintedAt(Instant lastPrintedAt) {
        this.lastPrintedAt = lastPrintedAt;
    }

}
