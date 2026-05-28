package com.example.wms.entity.erp;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.Instant;

// 库存流水实体（ERP进销存）
@TableName("erp_stock_txn")
public class ErpStockTxn {
    // 主键
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    // 租户ID
    @TableField("tenant_id")
    private Long tenantId;

    // 流水号
    @TableField("txn_no")
    private String txnNo;

    // 业务类型
    @TableField("biz_type")
    private String bizType;

    // 业务单据ID
    @TableField("biz_id")
    private Long bizId;

    // 业务明细ID
    @TableField("biz_item_id")
    private Long bizItemId;

    // 商品ID
    @TableField("product_id")
    private Long productId;

    // 仓库ID
    @TableField("warehouse_id")
    private Long warehouseId;

    // 库位ID
    @TableField("location_id")
    private Long locationId;

    // 变更数量
    @TableField("qty_delta")
    private BigDecimal qtyDelta;

    // 变更前数量
    @TableField("qty_before")
    private BigDecimal qtyBefore;

    // 变更后数量
    @TableField("qty_after")
    private BigDecimal qtyAfter;

    // 单位成本
    @TableField("unit_cost")
    private BigDecimal unitCost;

    // 成本金额
    @TableField("total_cost")
    private BigDecimal totalCost;

    // 操作者
    @TableField("operator")
    private String operator;

    // 操作者ID
    @TableField("operator_id")
    private Long operatorId;

    // 备注
    @TableField("remark")
    private String remark;

    // 创建时间
    @TableField("created_at")
    private Instant createdAt;

    // 关联单据号（非数据库字段，用于库存流水展示）
    @TableField(exist = false)
    private String docNo;

    // 库存调整原因（非数据库字段，用于库存流水展示）
    @TableField(exist = false)
    private String adjustmentReason;

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

    public String getTxnNo() {
        return txnNo;
    }

    public void setTxnNo(String txnNo) {
        this.txnNo = txnNo;
    }

    public String getBizType() {
        return bizType;
    }

    public void setBizType(String bizType) {
        this.bizType = bizType;
    }

    public Long getBizId() {
        return bizId;
    }

    public void setBizId(Long bizId) {
        this.bizId = bizId;
    }

    public Long getBizItemId() {
        return bizItemId;
    }

    public void setBizItemId(Long bizItemId) {
        this.bizItemId = bizItemId;
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

    public BigDecimal getQtyDelta() {
        return qtyDelta;
    }

    public void setQtyDelta(BigDecimal qtyDelta) {
        this.qtyDelta = qtyDelta;
    }

    public BigDecimal getQtyBefore() {
        return qtyBefore;
    }

    public void setQtyBefore(BigDecimal qtyBefore) {
        this.qtyBefore = qtyBefore;
    }

    public BigDecimal getQtyAfter() {
        return qtyAfter;
    }

    public void setQtyAfter(BigDecimal qtyAfter) {
        this.qtyAfter = qtyAfter;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(BigDecimal unitCost) {
        this.unitCost = unitCost;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(BigDecimal totalCost) {
        this.totalCost = totalCost;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public Long getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(Long operatorId) {
        this.operatorId = operatorId;
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

    public String getDocNo() {
        return docNo;
    }

    public void setDocNo(String docNo) {
        this.docNo = docNo;
    }

    public String getAdjustmentReason() {
        return adjustmentReason;
    }

    public void setAdjustmentReason(String adjustmentReason) {
        this.adjustmentReason = adjustmentReason;
    }
}
