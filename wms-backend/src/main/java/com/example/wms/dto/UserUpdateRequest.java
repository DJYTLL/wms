package com.example.wms.dto;

import jakarta.validation.constraints.NotBlank;

/**

 * 用户用于接收更新操作的请求参数。

 */
public record UserUpdateRequest(
    /**
     * 表示用户登录名。
     */
    @NotBlank String username,
    /**
     * 表示用户显示名称。
     */
    String displayName,
    /**
     * 表示邮箱地址。
     */
    String email,
    /**
     * 表示联系电话。
     */
    String phone,
    /**
     * 表示头像地址。
     */
    String avatarUrl,
    /**
     * 表示是否启用。
     */
    Boolean enabled,
    /**
     * 表示账户是否未过期。
     */
    Boolean accountNonExpired,
    /**
     * 表示账户是否未锁定。
     */
    Boolean accountNonLocked,
    /**
     * 表示登录凭证是否未过期。
     */
    Boolean credentialsNonExpired
) {
}
