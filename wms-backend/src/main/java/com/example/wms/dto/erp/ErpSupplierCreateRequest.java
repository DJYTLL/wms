package com.example.wms.dto.erp;

import jakarta.validation.constraints.NotBlank;

/**

 * ERP 供应商用于接收新增操作的请求参数。

 */
public record ErpSupplierCreateRequest(
    /**
     * 表示业务编码。
     */
    @NotBlank String code,
    /**
     * 表示名称。
     */
    @NotBlank String name,
    /**
     * 表示简称名称。
     */
    String shortName,
    /**
     * 表示供应商类型ID。
     */
    Long supplierTypeId,
    /**
     * 表示联系人。
     */
    String contact,
    /**
     * 表示联系电话。
     */
    String phone,
    /**
     * 表示手机号。
     */
    String mobile,
    /**
     * 表示邮箱地址。
     */
    String email,
    /**
     * 表示地址。
     */
    String address,
    /**
     * 表示区域。
     */
    String region,
    /**
     * 表示微信客服。
     */
    String wechat,
    /**
     * 表示采购员。
     */
    String purchaser,
    /**
     * 表示原始联系方式。
     */
    String contactInfo,
    /**
     * 表示税务编号。
     */
    String taxNo,
    /**
     * 表示银行名称。
     */
    String bankName,
    /**
     * 表示银行账户。
     */
    String bankAccount,
    /**
     * 表示默认结算方式编码。
     */
    String defaultSettlementMethodCode,
    /**
     * 表示默认付款方式编码。
     */
    String defaultPaymentMethodCode,
    /**
     * 表示联系人信息。
     */
    String contacts,
    /**
     * 表示是否启用。
     */
    Boolean enabled,
    /**
     * 表示拉黑。
     */
    Boolean blacklisted,
    /**
     * 表示来源创建时间。
     */
    String sourceCreatedAt,
    /**
     * 表示来源创建人。
     */
    String sourceCreatedBy,
    /**
     * 表示往来类别。
     */
    String businessScope,
    /**
     * 表示往来主体ID。
     */
    Long counterpartySubjectId,
    /**
     * 表示备注说明。
     */
    String remark
) {
}
