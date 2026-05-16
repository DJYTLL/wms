package com.example.wms.entity.erp;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.wms.entity.base.TenantAuditableSoftDeleteEntity;

import java.time.Instant;

// 库存盘点单实体（ERP进销存）
@TableName("erp_stock_count")
public class ErpStockCount extends TenantAuditableSoftDeleteEntity {
    // 盘点单号
    @TableField("count_no")
    private String countNo;

    // 盘点类型（COUNT/INIT）
    @TableField("count_type")
    private String countType;

    // 状态（DRAFT/APPROVED/CANCELLED/RED_FLUSHED）
    @TableField("status")
    private String status;

    // 仓库ID
    @TableField("warehouse_id")
    private Long warehouseId;

    // 库位ID
    @TableField("location_id")
    private Long locationId;

    // 调整原因
    @TableField("adjustment_reason")
    private String adjustmentReason;

    // 盘点时间
    @TableField("count_at")
    private Instant countAt;

    // 备注
    @TableField("remark")
    private String remark;

    @TableField("print_count")
    private Integer printCount;

    @TableField("last_printed_at")
    private Instant lastPrintedAt;

    // 制单人
    @TableField("created_by")
    private String createdBy;

    // 最后修改人
    @TableField("updated_by")
    private String updatedBy;

    // 审核人
    @TableField("approved_by")
    private String approvedBy;

    // 审核时间
    @TableField("approved_at")
    private Instant approvedAt;

    // 作废人
    @TableField("cancelled_by")
    private String cancelledBy;

    // 作废时间
    @TableField("cancelled_at")
    private Instant cancelledAt;

    public String getCountNo() {
        return countNo;
    }

    public void setCountNo(String countNo) {
        this.countNo = countNo;
    }

    public String getCountType() {
        return countType;
    }

    public void setCountType(String countType) {
        this.countType = countType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public Long getLocationId() {
        return locationId;
    }

    public void setLocationId(Long locationId) {
        this.locationId = locationId;
    }

    public String getAdjustmentReason() {
        return adjustmentReason;
    }

    public void setAdjustmentReason(String adjustmentReason) {
        this.adjustmentReason = adjustmentReason;
    }

    public Instant getCountAt() {
        return countAt;
    }

    public void setCountAt(Instant countAt) {
        this.countAt = countAt;
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

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
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

}
