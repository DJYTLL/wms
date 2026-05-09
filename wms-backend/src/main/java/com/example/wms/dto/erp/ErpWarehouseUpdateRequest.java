package com.example.wms.dto.erp;

import jakarta.validation.constraints.NotBlank;

// 更新仓库请求（ERP进销存）
public record ErpWarehouseUpdateRequest(
    @NotBlank String code,
    @NotBlank String name,
    String address,
    String manager,
    String phone,
    Boolean enabled,
    String remark
) {
}
