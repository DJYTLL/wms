package com.example.wms.entity.erp;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.Instant;

// 打印日志实体（ERP进销存）
@TableName("erp_print_log")
public class ErpPrintLog {
    // 主键
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    // 租户ID
    @TableField("tenant_id")
    private Long tenantId;

    // 单据类型
    @TableField("doc_type")
    private String docType;

    // 单据ID
    @TableField("doc_id")
    private Long docId;

    // 单据编号
    @TableField("doc_no")
    private String docNo;

    // 模板ID
    @TableField("template_id")
    private Long templateId;

    // 打印人
    @TableField("printed_by")
    private String printedBy;

    // 打印时间
    @TableField("printed_at")
    private Instant printedAt;

    // 客户端IP
    @TableField("client_ip")
    private String clientIp;

    // 客户端UA
    @TableField("user_agent")
    private String userAgent;

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

    public String getDocType() {
        return docType;
    }

    public void setDocType(String docType) {
        this.docType = docType;
    }

    public Long getDocId() {
        return docId;
    }

    public void setDocId(Long docId) {
        this.docId = docId;
    }

    public String getDocNo() {
        return docNo;
    }

    public void setDocNo(String docNo) {
        this.docNo = docNo;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    public String getPrintedBy() {
        return printedBy;
    }

    public void setPrintedBy(String printedBy) {
        this.printedBy = printedBy;
    }

    public Instant getPrintedAt() {
        return printedAt;
    }

    public void setPrintedAt(Instant printedAt) {
        this.printedAt = printedAt;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
}
