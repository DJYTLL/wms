package com.example.wms.dto;

import java.util.List;

/**

 * 用户声明信息，用于表示写入令牌中的当前用户身份与角色数据。

 */
public record UserClaim(
    /**
     * 表示用户登录名。
     */
    String username,
    /**
     * 表示角色标识。
     */
    String role,
    /**
     * 表示头像。
     */
    String avatar,
    /**
     * 表示角色列表。
     */
    List<String> roles
) {
}
