package com.example.wms.entity.erp;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.wms.entity.base.TenantAuditableSoftDeleteEntity;

import java.math.BigDecimal;
import java.time.Instant;

// ERP应收单实体
@TableName("erp_accounts_receivable")
public class ErpAccountsReceivable extends TenantAuditableSoftDeleteEntity {
    @TableField("sale_order_id")
    private Long saleOrderId;

    @TableField("order_no")
    private String orderNo;

    @TableField("customer_id")
    private Long customerId;

    @TableField("total_amount")
    private BigDecimal totalAmount;

    @TableField("paid_amount")
    private BigDecimal paidAmount;

    @TableField("unpaid_amount")
    private BigDecimal unpaidAmount;

    @TableField("status")
    private String status;

    @TableField("settlement_method")
    private String settlementMethod;

    @TableField("source_type")
    private String sourceType;

    @TableField("source_id")
    private Long sourceId;

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

    public Long getSaleOrderId() {
        return saleOrderId;
    }

    public void setSaleOrderId(Long saleOrderId) {
        this.saleOrderId = saleOrderId;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(BigDecimal paidAmount) {
        this.paidAmount = paidAmount;
    }

    public BigDecimal getUnpaidAmount() {
        return unpaidAmount;
    }

    public void setUnpaidAmount(BigDecimal unpaidAmount) {
        this.unpaidAmount = unpaidAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSettlementMethod() {
        return settlementMethod;
    }

    public void setSettlementMethod(String settlementMethod) {
        this.settlementMethod = settlementMethod;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public void setSourceId(Long sourceId) {
        this.sourceId = sourceId;
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
