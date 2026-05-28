package com.example.wms.entity.erp;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.Instant;

// Assembly/disassembly template entity
@TableName("erp_assembly_template")
public class ErpAssemblyTemplate {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("order_type")
    private String orderType;

    @TableField("name")
    private String name;

    @TableField("finished_product_id")
    private Long finishedProductId;

    @TableField("finished_qty")
    private BigDecimal finishedQty;

    @TableField("warehouse_id")
    private Long warehouseId;

    @TableField("location_id")
    private Long locationId;

    @TableField("labor_cost")
    private BigDecimal laborCost;

    @TableField("remark")
    private String remark;

    @TableField("created_at")
    private Instant createdAt;

    @TableField("updated_at")
    private Instant updatedAt;

    @TableField(value = "deleted_by", fill = FieldFill.UPDATE)
    private String deletedBy;

    @TableField(value = "delete_reason", fill = FieldFill.UPDATE)
    private String deleteReason;

    @TableLogic(value = "null", delval = "now()")
    @TableField("deleted_at")
    private Instant deletedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public String getOrderType() { return orderType; }
    public void setOrderType(String orderType) { this.orderType = orderType; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getFinishedProductId() { return finishedProductId; }
    public void setFinishedProductId(Long finishedProductId) { this.finishedProductId = finishedProductId; }
    public BigDecimal getFinishedQty() { return finishedQty; }
    public void setFinishedQty(BigDecimal finishedQty) { this.finishedQty = finishedQty; }
    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }
    public Long getLocationId() { return locationId; }
    public void setLocationId(Long locationId) { this.locationId = locationId; }
    public BigDecimal getLaborCost() { return laborCost; }
    public void setLaborCost(BigDecimal laborCost) { this.laborCost = laborCost; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public String getDeletedBy() { return deletedBy; }
    public void setDeletedBy(String deletedBy) { this.deletedBy = deletedBy; }
    public String getDeleteReason() { return deleteReason; }
    public void setDeleteReason(String deleteReason) { this.deleteReason = deleteReason; }
    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }
}
