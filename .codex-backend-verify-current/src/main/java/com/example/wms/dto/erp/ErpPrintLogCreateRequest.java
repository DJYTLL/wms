package com.example.wms.dto.erp;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**

 * ERP 打印日志用于接收新增操作的请求参数。

 */
public record ErpPrintLogCreateRequest(
    /**
     * 表示doc类型。
     */
    @NotBlank String docType,
    /**
     * 表示doc ID。
     */
    @NotNull Long docId,
    /**
     * 表示模板 ID。
     */
    Long templateId
) {
}
