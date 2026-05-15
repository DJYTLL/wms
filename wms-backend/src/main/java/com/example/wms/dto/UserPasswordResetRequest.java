package com.example.wms.dto;

import jakarta.validation.constraints.NotBlank;

/**

 * 用户密码重置用于接收接口请求参数。

 */
public record UserPasswordResetRequest(
    /**
     * 表示新密码。
     */
    @NotBlank String newPassword
) {
}
