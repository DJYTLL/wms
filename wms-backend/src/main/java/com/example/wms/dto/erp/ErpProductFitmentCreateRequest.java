package com.example.wms.dto.erp;

import jakarta.validation.constraints.NotNull;

// 新增商品适配车型请求（ERP进销存）
public record ErpProductFitmentCreateRequest(
    @NotNull Long productId,
    @NotNull Long modelId,
    String remark
) {
}
