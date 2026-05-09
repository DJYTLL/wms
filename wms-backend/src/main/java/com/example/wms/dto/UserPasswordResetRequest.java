package com.example.wms.dto;

import jakarta.validation.constraints.NotBlank;

// 重置密码请求
public record UserPasswordResetRequest(
    @NotBlank String newPassword
) {
}
