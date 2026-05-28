package com.example.wms.entity.erp;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.Instant;

// 采购退货明细实体（ERP进销存）
@TableName("erp_purchase_return_item")
public class ErpPurchaseReturnItem {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("return_id")
    private Long returnId;

    @TableField("product_id")
    private Long productId;

    @TableField("source_purchase_order_item_id")
    private Long sourcePurchaseOrderItemId;

    @TableField("source_purchase_order_id")
    private Long sourcePurchaseOrderId;

    @TableField(exist = false)
    private String sourcePurchaseOrderNo;

    @TableField(exist = false)
    private Integer sourcePurchaseOrderItemSortNo;

    @TableField(exist = false)
    private BigDecimal sourcePurchaseOrderItemQty;

    @TableField(exist = false)
    private BigDecimal sourcePurchaseOrderItemRemainingQty;

    @TableField(exist = false)
    private BigDecimal sourcePurchaseOrderItemApprovedReturnedQty;

    @TableField(exist = false)
    private BigDecimal sourcePurchaseOrderItemDraftOccupiedQty;

    @TableField("product_code")
    private String productCode;

    @TableField("product_name")
    private String productName;

    @TableField("warehouse_id")
    private Long warehouseId;

    @TableField("location_id")
    private Long locationId;

    @TableField("qty")
    private BigDecimal qty;

    @TableField("price")
    private BigDecimal price;

    @TableField("price_incl_tax")
    private BigDecimal priceInclTax;

    @TableField("amount")
    private BigDecimal amount;

    @TableField("amount_incl_tax")
    private BigDecimal amountInclTax;

    @TableField("tax_rate")
    private BigDecimal taxRate;

    @TableField("tax_amount")
    private BigDecimal taxAmount;

    @TableField("sort_no")
    private Integer sortNo;

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

    public Long getReturnId() {
        return returnId;
    }

    public void setReturnId(Long returnId) {
        this.returnId = returnId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Long getSourcePurchaseOrderItemId() {
        return sourcePurchaseOrderItemId;
    }

    public void setSourcePurchaseOrderItemId(Long sourcePurchaseOrderItemId) {
        this.sourcePurchaseOrderItemId = sourcePurchaseOrderItemId;
    }

    public Long getSourcePurchaseOrderId() {
        return sourcePurchaseOrderId;
    }

    public void setSourcePurchaseOrderId(Long sourcePurchaseOrderId) {
        this.sourcePurchaseOrderId = sourcePurchaseOrderId;
    }

    public String getSourcePurchaseOrderNo() {
        return sourcePurchaseOrderNo;
    }

    public void setSourcePurchaseOrderNo(String sourcePurchaseOrderNo) {
        this.sourcePurchaseOrderNo = sourcePurchaseOrderNo;
    }

    public Integer getSourcePurchaseOrderItemSortNo() {
        return sourcePurchaseOrderItemSortNo;
    }

    public void setSourcePurchaseOrderItemSortNo(Integer sourcePurchaseOrderItemSortNo) {
        this.sourcePurchaseOrderItemSortNo = sourcePurchaseOrderItemSortNo;
    }

    public BigDecimal getSourcePurchaseOrderItemQty() {
        return sourcePurchaseOrderItemQty;
    }

    public void setSourcePurchaseOrderItemQty(BigDecimal sourcePurchaseOrderItemQty) {
        this.sourcePurchaseOrderItemQty = sourcePurchaseOrderItemQty;
    }

    public BigDecimal getSourcePurchaseOrderItemRemainingQty() {
        return sourcePurchaseOrderItemRemainingQty;
    }

    public void setSourcePurchaseOrderItemRemainingQty(BigDecimal sourcePurchaseOrderItemRemainingQty) {
        this.sourcePurchaseOrderItemRemainingQty = sourcePurchaseOrderItemRemainingQty;
    }

    public BigDecimal getSourcePurchaseOrderItemApprovedReturnedQty() {
        return sourcePurchaseOrderItemApprovedReturnedQty;
    }

    public void setSourcePurchaseOrderItemApprovedReturnedQty(BigDecimal sourcePurchaseOrderItemApprovedReturnedQty) {
        this.sourcePurchaseOrderItemApprovedReturnedQty = sourcePurchaseOrderItemApprovedReturnedQty;
    }

    public BigDecimal getSourcePurchaseOrderItemDraftOccupiedQty() {
        return sourcePurchaseOrderItemDraftOccupiedQty;
    }

    public void setSourcePurchaseOrderItemDraftOccupiedQty(BigDecimal sourcePurchaseOrderItemDraftOccupiedQty) {
        this.sourcePurchaseOrderItemDraftOccupiedQty = sourcePurchaseOrderItemDraftOccupiedQty;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
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

    public BigDecimal getQty() {
        return qty;
    }

    public void setQty(BigDecimal qty) {
        this.qty = qty;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getPriceInclTax() {
        return priceInclTax;
    }

    public void setPriceInclTax(BigDecimal priceInclTax) {
        this.priceInclTax = priceInclTax;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getAmountInclTax() {
        return amountInclTax;
    }

    public void setAmountInclTax(BigDecimal amountInclTax) {
        this.amountInclTax = amountInclTax;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public Integer getSortNo() {
        return sortNo;
    }

    public void setSortNo(Integer sortNo) {
        this.sortNo = sortNo;
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
