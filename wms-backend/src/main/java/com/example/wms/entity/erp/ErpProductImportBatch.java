package com.example.wms.entity.erp;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.wms.mybatis.JsonbTypeHandler;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

@TableName(value = "erp_product_import_batch", autoResultMap = true)
public class ErpProductImportBatch {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("tenant_id")
    private Long tenantId;
    @TableField("batch_no")
    private String batchNo;
    @TableField("source_name")
    private String sourceName;
    @TableField("import_mode")
    private String importMode;
    @TableField("total_count")
    private Integer totalCount;
    @TableField("success_count")
    private Integer successCount;
    @TableField("failed_count")
    private Integer failedCount;
    @TableField("status")
    private String status;
    @TableField("summary")
    private String summary;
    @TableField(value = "raw_payload", typeHandler = JsonbTypeHandler.class)
    private JsonNode rawPayload;
    @TableField("created_by")
    private String createdBy;
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
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }
    public String getImportMode() { return importMode; }
    public void setImportMode(String importMode) { this.importMode = importMode; }
    public Integer getTotalCount() { return totalCount; }
    public void setTotalCount(Integer totalCount) { this.totalCount = totalCount; }
    public Integer getSuccessCount() { return successCount; }
    public void setSuccessCount(Integer successCount) { this.successCount = successCount; }
    public Integer getFailedCount() { return failedCount; }
    public void setFailedCount(Integer failedCount) { this.failedCount = failedCount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public JsonNode getRawPayload() { return rawPayload; }
    public void setRawPayload(JsonNode rawPayload) { this.rawPayload = rawPayload; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }
}
