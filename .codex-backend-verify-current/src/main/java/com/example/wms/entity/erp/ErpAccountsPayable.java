package com.example.wms.entity.erp;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.wms.entity.base.TenantAuditableSoftDeleteEntity;

import java.math.BigDecimal;
import java.time.Instant;

// 应付单实体（ERP进销存）
@TableName("erp_accounts_payable")
public class ErpAccountsPayable extends TenantAuditableSoftDeleteEntity {
    @TableField("purchase_order_id")
    private Long purchaseOrderId;

    @TableField("purchase_return_id")
    private Long purchaseReturnId;

    @TableField("order_no")
    private String orderNo;

    @TableField("supplier_id")
    private Long supplierId;

    @TableField("total_amount")
    private BigDecimal totalAmount;

    @TableField("paid_amount")
    private BigDecimal paidAmount;

    @TableField("discount_amount")
    private BigDecimal discountAmount;

    @TableField("unpaid_amount")
    private BigDecimal unpaidAmount;

    @TableField("status")
    private String status;

    @TableField("settlement_method")
    private String settlementMethod;

    @TableField("remark")
    private String remark;

    @TableField("source_document_type")
    private String sourceDocumentType;

    @TableField("source_document_id")
    private Long sourceDocumentId;

    @TableField("source_business_flow")
    private String sourceBusinessFlow;

    @TableField("auto_flow_generated")
    private Boolean autoFlowGenerated;

    @TableField("auto_flow_mode")
    private String autoFlowMode;

    @TableField("auto_flow_managed_state")
    private String autoFlowManagedState;

    @TableField("print_count")
    private Integer printCount;

    @TableField("last_printed_at")
    private Instant lastPrintedAt;

    public Long getPurchaseOrderId() {
        return purchaseOrderId;
    }

    public void setPurchaseOrderId(Long purchaseOrderId) {
        this.purchaseOrderId = purchaseOrderId;
    }

    public Long getPurchaseReturnId() {
        return purchaseReturnId;
    }

    public void setPurchaseReturnId(Long purchaseReturnId) {
        this.purchaseReturnId = purchaseReturnId;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
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

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
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

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getSourceDocumentType() {
        return sourceDocumentType;
    }

    public void setSourceDocumentType(String sourceDocumentType) {
        this.sourceDocumentType = sourceDocumentType;
    }

    public Long getSourceDocumentId() {
        return sourceDocumentId;
    }

    public void setSourceDocumentId(Long sourceDocumentId) {
        this.sourceDocumentId = sourceDocumentId;
    }

    public String getSourceBusinessFlow() {
        return sourceBusinessFlow;
    }

    public void setSourceBusinessFlow(String sourceBusinessFlow) {
        this.sourceBusinessFlow = sourceBusinessFlow;
    }

    public Boolean getAutoFlowGenerated() {
        return autoFlowGenerated;
    }

    public void setAutoFlowGenerated(Boolean autoFlowGenerated) {
        this.autoFlowGenerated = autoFlowGenerated;
    }

    public String getAutoFlowMode() {
        return autoFlowMode;
    }

    public void setAutoFlowMode(String autoFlowMode) {
        this.autoFlowMode = autoFlowMode;
    }

    public String getAutoFlowManagedState() {
        return autoFlowManagedState;
    }

    public void setAutoFlowManagedState(String autoFlowManagedState) {
        this.autoFlowManagedState = autoFlowManagedState;
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
