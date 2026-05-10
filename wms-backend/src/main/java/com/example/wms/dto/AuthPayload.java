package com.example.wms.dto;

import java.util.List;

// JWT 载荷：包含用户对象与权限列表
public record AuthPayload(UserClaim user,
                          List<String> permissions,
                          long authVersion,
                          Long tenantId,
                          String tenantCode,
                          Long userTenantId,
                          String userTenantCode) {
}
