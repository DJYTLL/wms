package com.example.wms.dto;

/**

 * 用户状态用于接收更新操作的请求参数。

 */
public record UserStatusUpdateRequest(
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
