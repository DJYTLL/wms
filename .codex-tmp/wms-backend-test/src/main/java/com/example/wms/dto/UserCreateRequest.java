package com.example.wms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**

 * 用户用于接收新增操作的请求参数。

 */
public record UserCreateRequest(
    /**
     * 表示用户登录名。
     */
    @NotBlank String username,
    /**
     * 表示用户密码。
     */
    @NotBlank String password,
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
    Boolean credentialsNonExpired,
    /**
     * 表示用户所属角色 ID 列表。
     */
    @NotEmpty(message = "角色列表不能为空") List<Long> roleIds
) {
}
