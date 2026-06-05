package com.example.wms.entity.erp;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.wms.mybatis.JsonbTypeHandler;
import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.Instant;

@TableName(value = "erp_stock_init_import_item", autoResultMap = true)
public class ErpStockInitImportItem {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("tenant_id")
    private Long tenantId;
    @TableField("batch_id")
    private Long batchId;
    @TableField("row_no")
    private Integer rowNo;
    @TableField("source_code")
    private String sourceCode;
    @TableField("source_name")
    private String sourceName;
    @TableField("matched_product_id")
    private Long matchedProductId;
    @TableField("warehouse_name")
    private String warehouseName;
    @TableField("location_name")
    private String locationName;
    @TableField("counted_qty")
    private BigDecimal countedQty;
    @TableField("init_unit_cost")
    private BigDecimal initUnitCost;
    @TableField("init_total_amount")
    private BigDecimal initTotalAmount;
    @TableField("status")
    private String status;
    @TableField("error_field")
    private String errorField;
    @TableField("error_message")
    private String errorMessage;
    @TableField("suggestion")
    private String suggestion;
    @TableField("warning_message")
    private String warningMessage;
    @TableField("matched_strategy")
    private String matchedStrategy;
    @TableField(value = "raw_row", typeHandler = JsonbTypeHandler.class)
    private JsonNode rawRow;
    @TableField(value = "normalized_payload", typeHandler = JsonbTypeHandler.class)
    private JsonNode normalizedPayload;
    @TableField("created_at")
    private Instant createdAt;
    @TableField("updated_at")
    private Instant updatedAt;
    @TableLogic(value = "null", delval = "now()")
    @TableField("deleted_at")
    private Instant deletedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }
    public Integer getRowNo() { return rowNo; }
    public void setRowNo(Integer rowNo) { this.rowNo = rowNo; }
    public String getSourceCode() { return sourceCode; }
    public void setSourceCode(String sourceCode) { this.sourceCode = sourceCode; }
    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }
    public Long getMatchedProductId() { return matchedProductId; }
    public void setMatchedProductId(Long matchedProductId) { this.matchedProductId = matchedProductId; }
    public String getWarehouseName() { return warehouseName; }
    public void setWarehouseName(String warehouseName) { this.warehouseName = warehouseName; }
    public String getLocationName() { return locationName; }
    public void setLocationName(String locationName) { this.locationName = locationName; }
    public BigDecimal getCountedQty() { return countedQty; }
    public void setCountedQty(BigDecimal countedQty) { this.countedQty = countedQty; }
    public BigDecimal getInitUnitCost() { return initUnitCost; }
    public void setInitUnitCost(BigDecimal initUnitCost) { this.initUnitCost = initUnitCost; }
    public BigDecimal getInitTotalAmount() { return initTotalAmount; }
    public void setInitTotalAmount(BigDecimal initTotalAmount) { this.initTotalAmount = initTotalAmount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getErrorField() { return errorField; }
    public void setErrorField(String errorField) { this.errorField = errorField; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public String getSuggestion() { return suggestion; }
    public void setSuggestion(String suggestion) { this.suggestion = suggestion; }
    public String getWarningMessage() { return warningMessage; }
    public void setWarningMessage(String warningMessage) { this.warningMessage = warningMessage; }
    public String getMatchedStrategy() { return matchedStrategy; }
    public void setMatchedStrategy(String matchedStrategy) { this.matchedStrategy = matchedStrategy; }
    public JsonNode getRawRow() { return rawRow; }
    public void setRawRow(JsonNode rawRow) { this.rawRow = rawRow; }
    public JsonNode getNormalizedPayload() { return normalizedPayload; }
    public void setNormalizedPayload(JsonNode normalizedPayload) { this.normalizedPayload = normalizedPayload; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }
}
