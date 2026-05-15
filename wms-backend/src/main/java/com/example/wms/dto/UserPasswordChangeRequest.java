package com.example.wms.dto;

import jakarta.validation.constraints.NotBlank;

/**

 * 用户密码修改用于接收接口请求参数。

 */
public record UserPasswordChangeRequest(
    /**
     * 表示原密码。
     */
    @NotBlank String oldPassword,
    /**
     * 表示新密码。
     */
    @NotBlank String newPassword
) {
}
