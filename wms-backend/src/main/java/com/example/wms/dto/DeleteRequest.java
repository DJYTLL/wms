package com.example.wms.dto;

import jakarta.validation.constraints.NotBlank;

// 通用删除请求：强制要求填写删除原因
public record DeleteRequest(
    @NotBlank(message = "删除原因不能为空")
    String reason
) {
}
