package com.example.wms.dto.erp;

import jakarta.validation.constraints.NotBlank;

/**

 * ERP 打印模板用于接收更新操作的请求参数。

 */
public record ErpPrintTemplateUpdateRequest(
    /**
     * 表示业务编码。
     */
    @NotBlank String code,
    /**
     * 表示名称。
     */
    @NotBlank String name,
    /**
     * 表示doc类型。
     */
    @NotBlank String docType,
    /**
     * 表示header标题。
     */
    String headerTitle,
    /**
     * 表示sub标题。
     */
    String subTitle,
    /**
     * 表示footerNote。
     */
    String footerNote,
    /**
     * 表示fieldConfig。
     */
    String fieldConfig,
    /**
     * 表示排序编号。
     */
    Integer sortNo,
    /**
     * 表示是否启用。
     */
    Boolean enabled,
    /**
     * 表示是否默认。
     */
    Boolean isDefault,
    /**
     * 表示备注说明。
     */
    String remark
) {
}
