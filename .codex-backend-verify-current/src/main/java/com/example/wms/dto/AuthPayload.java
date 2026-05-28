package com.example.wms.dto;

import java.util.List;

/**

 * 鉴权载荷对象，用于返回当前登录用户信息、权限列表和租户鉴权信息。

 */
public record AuthPayload(
    /**
     * 表示当前登录用户信息。
     */
    UserClaim user,
    /**
     * 表示当前用户拥有的权限列表。
     */
    List<String> permissions,
    /**
     * 表示鉴权版本号。
     */
    long authVersion,
    /**
     * 表示租户 ID。
     */
    Long tenantId,
    /**
     * 表示租户编码。
     */
    String tenantCode,
    /**
     * 表示用户所属租户 ID。
     */
    Long userTenantId,
    /**
     * 表示用户所属租户编码。
     */
    String userTenantCode
) {
}
