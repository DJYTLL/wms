package com.example.wms.entity.erp;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.wms.entity.base.TenantAuditableSoftDeleteEntity;

import java.math.BigDecimal;
import java.time.Instant;

// 付款单实体（ERP进销存）
@TableName("erp_payment")
public class ErpPayment extends TenantAuditableSoftDeleteEntity {
    @TableField("payable_id")
    private Long payableId;

    @TableField("purchase_order_id")
    private Long purchaseOrderId;

    @TableField("payment_no")
    private String paymentNo;

    @TableField("supplier_id")
    private Long supplierId;

    @TableField("amount")
    private BigDecimal amount;

    @TableField("discount_amount")
    private BigDecimal discountAmount;

    @TableField("settlement_method")
    private String settlementMethod;

    @TableField("payment_method_code")
    private String paymentMethodCode;

    @TableField("status")
    private String status;

    @TableField("paid_at")
    private Instant paidAt;

    @TableField("remark")
    private String remark;

    @TableField("print_count")
    private Integer printCount;

    @TableField("last_printed_at")
    private Instant lastPrintedAt;

    public Long getPayableId() {
        return payableId;
    }

    public void setPayableId(Long payableId) {
        this.payableId = payableId;
    }

    public Long getPurchaseOrderId() {
        return purchaseOrderId;
    }

    public void setPurchaseOrderId(Long purchaseOrderId) {
        this.purchaseOrderId = purchaseOrderId;
    }

    public String getPaymentNo() {
        return paymentNo;
    }

    public void setPaymentNo(String paymentNo) {
        this.paymentNo = paymentNo;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
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

    public String getPaymentMethodCode() {
        return paymentMethodCode;
    }

    public void setPaymentMethodCode(String paymentMethodCode) {
        this.paymentMethodCode = paymentMethodCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(Instant paidAt) {
        this.paidAt = paidAt;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
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
