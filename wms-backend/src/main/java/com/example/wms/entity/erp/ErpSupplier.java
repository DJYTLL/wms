package com.example.wms.entity.erp;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.JsonNode;
import com.example.wms.mybatis.JsonbTypeHandler;

import java.time.Instant;

// 供应商实体（ERP进销存）
@TableName(value = "erp_supplier", autoResultMap = true)
public class ErpSupplier {
    // 主键
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    // 租户ID
    @TableField("tenant_id")
    private Long tenantId;

    // 供应商编码
    @TableField("code")
    private String code;

    // 供应商名称
    @TableField("name")
    private String name;

    // 供应商简称
    @TableField("short_name")
    private String shortName;

    // 供应商类型ID
    @TableField("supplier_type_id")
    private Long supplierTypeId;

    // 联系人
    @TableField("contact")
    private String contact;

    // 联系电话
    @TableField("phone")
    private String phone;

    // 联系手机
    @TableField("mobile")
    private String mobile;

    // 邮箱
    @TableField("email")
    private String email;

    // 地址
    @TableField("address")
    private String address;

    // 区域
    @TableField("region")
    private String region;

    // 微信客服
    @TableField("wechat")
    private String wechat;

    // 采购员
    @TableField("purchaser")
    private String purchaser;

    // 原始联系方式
    @TableField("contact_info")
    private String contactInfo;

    // 税号
    @TableField("tax_no")
    private String taxNo;

    // 开户行
    @TableField("bank_name")
    private String bankName;

    // 银行账号
    @TableField("bank_account")
    private String bankAccount;

    // 默认结算方式编码
    @TableField("default_settlement_method_code")
    private String defaultSettlementMethodCode;

    // 默认付款方式编码
    @TableField("default_payment_method_code")
    private String defaultPaymentMethodCode;

    // 联系人列表(JSON)
    @TableField(value = "contacts", typeHandler = JsonbTypeHandler.class)
    private JsonNode contacts;

    // 是否启用
    @TableField("is_enabled")
    private Boolean enabled;

    // 是否黑名单
    @TableField("is_blacklisted")
    private Boolean blacklisted;

    // 备注
    @TableField("remark")
    private String remark;

    // 来源创建时间
    @TableField("source_created_at")
    private Instant sourceCreatedAt;

    // 来源创建人
    @TableField("source_created_by")
    private String sourceCreatedBy;

    // 往来类别
    @TableField("business_scope")
    private String businessScope;

    // 往来主体ID
    @TableField("counterparty_subject_id")
    private Long counterpartySubjectId;

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

    @TableField(exist = false)
    private Instant recentTransactionAt;

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

    public Long getSupplierTypeId() {
        return supplierTypeId;
    }

    public void setSupplierTypeId(Long supplierTypeId) {
        this.supplierTypeId = supplierTypeId;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getWechat() {
        return wechat;
    }

    public void setWechat(String wechat) {
        this.wechat = wechat;
    }

    public String getPurchaser() {
        return purchaser;
    }

    public void setPurchaser(String purchaser) {
        this.purchaser = purchaser;
    }

    public String getContactInfo() {
        return contactInfo;
    }

    public void setContactInfo(String contactInfo) {
        this.contactInfo = contactInfo;
    }

    public String getTaxNo() {
        return taxNo;
    }

    public void setTaxNo(String taxNo) {
        this.taxNo = taxNo;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getBankAccount() {
        return bankAccount;
    }

    public void setBankAccount(String bankAccount) {
        this.bankAccount = bankAccount;
    }

    public String getDefaultSettlementMethodCode() {
        return defaultSettlementMethodCode;
    }

    public void setDefaultSettlementMethodCode(String defaultSettlementMethodCode) {
        this.defaultSettlementMethodCode = defaultSettlementMethodCode;
    }

    public String getDefaultPaymentMethodCode() {
        return defaultPaymentMethodCode;
    }

    public void setDefaultPaymentMethodCode(String defaultPaymentMethodCode) {
        this.defaultPaymentMethodCode = defaultPaymentMethodCode;
    }

    public JsonNode getContacts() {
        return contacts;
    }

    public void setContacts(JsonNode contacts) {
        this.contacts = contacts;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getBlacklisted() {
        return blacklisted;
    }

    public void setBlacklisted(Boolean blacklisted) {
        this.blacklisted = blacklisted;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Instant getSourceCreatedAt() {
        return sourceCreatedAt;
    }

    public void setSourceCreatedAt(Instant sourceCreatedAt) {
        this.sourceCreatedAt = sourceCreatedAt;
    }

    public String getSourceCreatedBy() {
        return sourceCreatedBy;
    }

    public void setSourceCreatedBy(String sourceCreatedBy) {
        this.sourceCreatedBy = sourceCreatedBy;
    }

    public String getBusinessScope() {
        return businessScope;
    }

    public void setBusinessScope(String businessScope) {
        this.businessScope = businessScope;
    }

    public Long getCounterpartySubjectId() {
        return counterpartySubjectId;
    }

    public void setCounterpartySubjectId(Long counterpartySubjectId) {
        this.counterpartySubjectId = counterpartySubjectId;
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

    public Instant getRecentTransactionAt() {
        return recentTransactionAt;
    }

    public void setRecentTransactionAt(Instant recentTransactionAt) {
        this.recentTransactionAt = recentTransactionAt;
    }
}
