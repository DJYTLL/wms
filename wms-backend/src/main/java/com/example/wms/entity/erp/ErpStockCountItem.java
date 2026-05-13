package com.example.wms.entity.erp;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.Instant;

// 库存盘点明细实体（ERP进销存）
@TableName("erp_stock_count_item")
public class ErpStockCountItem {
    // 主键
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    // 租户ID
    @TableField("tenant_id")
    private Long tenantId;

    // 盘点单ID
    @TableField("count_id")
    private Long countId;

    // 明细排序
    @TableField("line_no")
    private Integer lineNo;

    // 商品ID
    @TableField("product_id")
    private Long productId;

    // 仓库ID
    @TableField("warehouse_id")
    private Long warehouseId;

    // 库位ID
    @TableField("location_id")
    private Long locationId;

    // 系统数量
    @TableField("system_qty")
    private BigDecimal systemQty;

    // 盘点数量
    @TableField("counted_qty")
    private BigDecimal countedQty;

    // 期初单价
    @TableField("init_unit_cost")
    private BigDecimal initUnitCost;

    // 期初金额
    @TableField("init_total_amount")
    private BigDecimal initTotalAmount;

    // 差异数量
    @TableField("diff_qty")
    private BigDecimal diffQty;

    // 备注
    @TableField("remark")
    private String remark;

    // 创建时间
    @TableField("created_at")
    private Instant createdAt;

    // 更新时间
    @TableField("updated_at")
    private Instant updatedAt;

    @TableField(value = "deleted_by", fill = FieldFill.UPDATE)
    private String deletedBy;

    @TableField(value = "delete_reason", fill = FieldFill.UPDATE)
    private String deleteReason;

    // 删除时间
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

    public Long getCountId() {
        return countId;
    }

    public void setCountId(Long countId) {
        this.countId = countId;
    }

    public Integer getLineNo() {
        return lineNo;
    }

    public void setLineNo(Integer lineNo) {
        this.lineNo = lineNo;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
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

    public BigDecimal getSystemQty() {
        return systemQty;
    }

    public void setSystemQty(BigDecimal systemQty) {
        this.systemQty = systemQty;
    }

    public BigDecimal getCountedQty() {
        return countedQty;
    }

    public void setCountedQty(BigDecimal countedQty) {
        this.countedQty = countedQty;
    }

    public BigDecimal getInitUnitCost() {
        return initUnitCost;
    }

    public void setInitUnitCost(BigDecimal initUnitCost) {
        this.initUnitCost = initUnitCost;
    }

    public BigDecimal getInitTotalAmount() {
        return initTotalAmount;
    }

    public void setInitTotalAmount(BigDecimal initTotalAmount) {
        this.initTotalAmount = initTotalAmount;
    }

    public BigDecimal getDiffQty() {
        return diffQty;
    }

    public void setDiffQty(BigDecimal diffQty) {
        this.diffQty = diffQty;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
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
