package com.example.wms.entity.erp;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.wms.mybatis.JsonbTypeHandler;
import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.Instant;

// 商品实体（ERP进销存）
@TableName(value = "erp_product", autoResultMap = true)
public class ErpProduct {
    // 主键
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    // 租户ID
    @TableField("tenant_id")
    private Long tenantId;

    // 商品编码
    @TableField("code")
    private String code;

    // 商品名称
    @TableField("name")
    private String name;

    // 商品简称
    @TableField("short_name")
    private String shortName;

    // 规格型号
    @TableField("spec")
    private String spec;

    // 型号
    @TableField("model")
    private String model;

    // 分类ID
    @TableField("category_id")
    private Long categoryId;

    // 单位ID
    @TableField("unit_id")
    private Long unitId;

    // 默认仓库ID
    @TableField("default_warehouse_id")
    private Long defaultWarehouseId;

    // 默认库位ID
    @TableField("default_location_id")
    private Long defaultLocationId;

    // 条码
    @TableField("barcode")
    private String barcode;

    // SKU
    @TableField("sku")
    private String sku;

    // 品牌
    @TableField("brand")
    private String brand;

    // 产地
    @TableField("origin")
    private String origin;

    // 重量
    @TableField("weight")
    private BigDecimal weight;

    // 体积
    @TableField("volume")
    private BigDecimal volume;

    // 成本价
    @TableField("cost_price")
    private BigDecimal costPrice;

    // 销售价
    @TableField("sale_price")
    private BigDecimal salePrice;

    // 默认税率
    @TableField("tax_rate")
    private BigDecimal taxRate;

    // 安全库存
    @TableField("safety_stock")
    private BigDecimal safetyStock;

    // 最低库存
    @TableField("min_stock")
    private BigDecimal minStock;

    // 最高库存
    @TableField("max_stock")
    private BigDecimal maxStock;

    // 是否批次管理
    @TableField("is_batch")
    private Boolean batch;

    // 保质期(天)
    @TableField("shelf_life_days")
    private Integer shelfLifeDays;

    // 是否启用
    @TableField("is_enabled")
    private Boolean enabled;

    // 扩展属性(JSON)
    @TableField(value = "ext_attrs", typeHandler = JsonbTypeHandler.class)
    private JsonNode extAttrs;

    // 备注
    @TableField("remark")
    private String remark;

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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public String getSpec() {
        return spec;
    }

    public void setSpec(String spec) {
        this.spec = spec;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Long getUnitId() {
        return unitId;
    }

    public void setUnitId(Long unitId) {
        this.unitId = unitId;
    }

    public Long getDefaultWarehouseId() {
        return defaultWarehouseId;
    }

    public void setDefaultWarehouseId(Long defaultWarehouseId) {
        this.defaultWarehouseId = defaultWarehouseId;
    }

    public Long getDefaultLocationId() {
        return defaultLocationId;
    }

    public void setDefaultLocationId(Long defaultLocationId) {
        this.defaultLocationId = defaultLocationId;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public void setWeight(BigDecimal weight) {
        this.weight = weight;
    }

    public BigDecimal getVolume() {
        return volume;
    }

    public void setVolume(BigDecimal volume) {
        this.volume = volume;
    }

    public BigDecimal getCostPrice() {
        return costPrice;
    }

    public void setCostPrice(BigDecimal costPrice) {
        this.costPrice = costPrice;
    }

    public BigDecimal getSalePrice() {
        return salePrice;
    }

    public void setSalePrice(BigDecimal salePrice) {
        this.salePrice = salePrice;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }

    public BigDecimal getSafetyStock() {
        return safetyStock;
    }

    public void setSafetyStock(BigDecimal safetyStock) {
        this.safetyStock = safetyStock;
    }

    public BigDecimal getMinStock() {
        return minStock;
    }

    public void setMinStock(BigDecimal minStock) {
        this.minStock = minStock;
    }

    public BigDecimal getMaxStock() {
        return maxStock;
    }

    public void setMaxStock(BigDecimal maxStock) {
        this.maxStock = maxStock;
    }

    public Boolean getBatch() {
        return batch;
    }

    public void setBatch(Boolean batch) {
        this.batch = batch;
    }

    public Integer getShelfLifeDays() {
        return shelfLifeDays;
    }

    public void setShelfLifeDays(Integer shelfLifeDays) {
        this.shelfLifeDays = shelfLifeDays;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public JsonNode getExtAttrs() {
        return extAttrs;
    }

    public void setExtAttrs(JsonNode extAttrs) {
        this.extAttrs = extAttrs;
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
