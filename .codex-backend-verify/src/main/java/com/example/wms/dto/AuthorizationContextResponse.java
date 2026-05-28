package com.example.wms.dto;

import java.util.List;

public record AuthorizationContextResponse(
    UserClaim user,
    List<String> permissions,
    long authVersion,
    Long tenantId,
    String tenantCode,
    Long userTenantId,
    String userTenantCode
) {
}
