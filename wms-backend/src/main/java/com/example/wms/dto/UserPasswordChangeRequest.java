package com.example.wms.dto;

import jakarta.validation.constraints.NotBlank;

// 修改密码请求
public record UserPasswordChangeRequest(
    @NotBlank String oldPassword,
    @NotBlank String newPassword
) {
}
