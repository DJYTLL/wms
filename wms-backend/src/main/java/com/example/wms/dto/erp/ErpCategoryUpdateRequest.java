package com.example.wms.dto.erp;

import jakarta.validation.constraints.NotBlank;

// 更新分类请求（ERP进销存）
public record ErpCategoryUpdateRequest(
    @NotBlank String code,
    @NotBlank String name,
    Long parentId,
    Integer level,
    Integer sortNo,
    Boolean enabled,
    String remark
) {
}
