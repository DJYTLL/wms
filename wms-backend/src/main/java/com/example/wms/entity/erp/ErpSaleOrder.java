package com.example.wms.entity.erp;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.Instant;

// 销售单实体（ERP进销存）
@TableName("erp_sale_order")
public class ErpSaleOrder {
    // 主键
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    // 租户ID
    @TableField("tenant_id")
    private Long tenantId;

    // 单号
    @TableField("order_no")
    private String orderNo;

    // 状态
    @TableField("status")
    private String status;

    // 客户ID
    @TableField("customer_id")
    private Long customerId;

    // 单据时间
    @TableField("order_at")
    private Instant orderAt;

    // 结算方式编码
    @TableField("settlement_method")
    private String settlementMethod;

    // 收款方式编码
    @TableField("receipt_method_code")
    private String receiptMethodCode;

    // 送货方式编码
    @TableField("delivery_method_code")
    private String deliveryMethod;

    // 付款金额
    @TableField("paid_amount")
    private BigDecimal paidAmount;

    // 优惠金额
    @TableField("discount_amount")
    private BigDecimal discountAmount;

    // 总金额
    @TableField("total_amount")
    private BigDecimal totalAmount;

    // 未税总金额
    @TableField("total_amount_excl_tax")
    private BigDecimal totalAmountExclTax;

    // 税额合计
    @TableField("total_tax_amount")
    private BigDecimal totalTaxAmount;

    // 含税总金额
    @TableField("total_amount_incl_tax")
    private BigDecimal totalAmountInclTax;

    // 乐观锁版本
    @TableField("version")
    private Long version;

    // 审核人
    @TableField("approved_by")
    private String approvedBy;

    // 审核时间
    @TableField("approved_at")
    private Instant approvedAt;

    // 草稿是否已占用库存
    @TableField("inventory_reserved")
    private Boolean inventoryReserved;

    // 反审核人
    @TableField("unapproved_by")
    private String unapprovedBy;

    // 反审核时间
    @TableField("unapproved_at")
    private Instant unapprovedAt;

    // 作废人
    @TableField("cancelled_by")
    private String cancelledBy;

    // 作废时间
    @TableField("cancelled_at")
    private Instant cancelledAt;

    // 打印次数
    @TableField("print_count")
    private Integer printCount;

    // 最后打印时间
    @TableField("last_printed_at")
    private Instant lastPrintedAt;

    // 备注
    @TableField("remark")
    private String remark;

    @TableField("red_flush_source_type")
    private String redFlushSourceType;

    @TableField("red_flush_source_id")
    private Long redFlushSourceId;

    @TableField(exist = false)
    private String receivableStatus;

    @TableField(exist = false)
    private BigDecimal receivableUnpaidAmount;

    @TableField(exist = false)
    private Long approvedReturnCount;

    @TableField(exist = false)
    private BigDecimal cumulativeReturnAmount;

    @TableField(exist = false)
    private BigDecimal cumulativeReturnCost;

    @TableField(exist = false)
    private BigDecimal netSaleAmount;

    @TableField(exist = false)
    private BigDecimal netGrossProfit;

    @TableField(exist = false)
    private String redFlushTrace;

    // 创建时间
    @TableField("created_at")
    private Instant createdAt;

    // 更新时间
    @TableField("updated_at")
    private Instant updatedAt;

    // 删除时间
    @TableField(value = "deleted_by", fill = FieldFill.UPDATE)
    private String deletedBy;

    @TableField(value = "delete_reason", fill = FieldFill.UPDATE)
    private String deleteReason;

    @TableLogic(value = "null", delval = "now()")
    @TableField("deleted_at")
    private Instant deletedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Instant getOrderAt() {
        return orderAt;
    }

    public void setOrderAt(Instant orderAt) {
        this.orderAt = orderAt;
    }

    public String getSettlementMethod() {
        return settlementMethod;
    }

    public void setSettlementMethod(String settlementMethod) {
        this.settlementMethod = settlementMethod;
    }

    public String getReceiptMethodCode() {
        return receiptMethodCode;
    }

    public void setReceiptMethodCode(String receiptMethodCode) {
        this.receiptMethodCode = receiptMethodCode;
    }

    public String getDeliveryMethod() {
        return deliveryMethod;
    }

    public void setDeliveryMethod(String deliveryMethod) {
        this.deliveryMethod = deliveryMethod;
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

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getTotalAmountExclTax() {
        return totalAmountExclTax;
    }

    public void setTotalAmountExclTax(BigDecimal totalAmountExclTax) {
        this.totalAmountExclTax = totalAmountExclTax;
    }

    public BigDecimal getTotalTaxAmount() {
        return totalTaxAmount;
    }

    public void setTotalTaxAmount(BigDecimal totalTaxAmount) {
        this.totalTaxAmount = totalTaxAmount;
    }

    public BigDecimal getTotalAmountInclTax() {
        return totalAmountInclTax;
    }

    public void setTotalAmountInclTax(BigDecimal totalAmountInclTax) {
        this.totalAmountInclTax = totalAmountInclTax;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(Instant approvedAt) {
        this.approvedAt = approvedAt;
    }

    public Boolean getInventoryReserved() {
        return inventoryReserved;
    }

    public void setInventoryReserved(Boolean inventoryReserved) {
        this.inventoryReserved = inventoryReserved;
    }

    public String getUnapprovedBy() {
        return unapprovedBy;
    }

    public void setUnapprovedBy(String unapprovedBy) {
        this.unapprovedBy = unapprovedBy;
    }

    public Instant getUnapprovedAt() {
        return unapprovedAt;
    }

    public void setUnapprovedAt(Instant unapprovedAt) {
        this.unapprovedAt = unapprovedAt;
    }

    public String getCancelledBy() {
        return cancelledBy;
    }

    public void setCancelledBy(String cancelledBy) {
        this.cancelledBy = cancelledBy;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Instant cancelledAt) {
        this.cancelledAt = cancelledAt;
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

    public String getReceivableStatus() {
        return receivableStatus;
    }

    public void setReceivableStatus(String receivableStatus) {
        this.receivableStatus = receivableStatus;
    }

    public BigDecimal getReceivableUnpaidAmount() {
        return receivableUnpaidAmount;
    }

    public void setReceivableUnpaidAmount(BigDecimal receivableUnpaidAmount) {
        this.receivableUnpaidAmount = receivableUnpaidAmount;
    }

    public Long getApprovedReturnCount() {
        return approvedReturnCount;
    }

    public void setApprovedReturnCount(Long approvedReturnCount) {
        this.approvedReturnCount = approvedReturnCount;
    }

    public BigDecimal getCumulativeReturnAmount() {
        return cumulativeReturnAmount;
    }

    public void setCumulativeReturnAmount(BigDecimal cumulativeReturnAmount) {
        this.cumulativeReturnAmount = cumulativeReturnAmount;
    }

    public BigDecimal getCumulativeReturnCost() {
        return cumulativeReturnCost;
    }

    public void setCumulativeReturnCost(BigDecimal cumulativeReturnCost) {
        this.cumulativeReturnCost = cumulativeReturnCost;
    }

    public BigDecimal getNetSaleAmount() {
        return netSaleAmount;
    }

    public void setNetSaleAmount(BigDecimal netSaleAmount) {
        this.netSaleAmount = netSaleAmount;
    }

    public BigDecimal getNetGrossProfit() {
        return netGrossProfit;
    }

    public void setNetGrossProfit(BigDecimal netGrossProfit) {
        this.netGrossProfit = netGrossProfit;
    }

    public String getRedFlushTrace() {
        return redFlushTrace;
    }

    public void setRedFlushTrace(String redFlushTrace) {
        this.redFlushTrace = redFlushTrace;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getDeletedBy() {
        return deletedBy;
    }

    public void setDeletedBy(String deletedBy) {
        this.deletedBy = deletedBy;
    }

    public String getDeleteReason() {
        return deleteReason;
    }

    public void setDeleteReason(String deleteReason) {
        this.deleteReason = deleteReason;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }
}
