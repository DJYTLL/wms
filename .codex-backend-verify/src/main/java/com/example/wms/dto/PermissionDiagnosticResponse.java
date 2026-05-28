package com.example.wms.dto;

import java.util.List;

/**
 * 权限诊断信息，用于展示权限定义的角色绑定、菜单引用和维护风险。
 */
public record PermissionDiagnosticResponse(
    Long permissionId,
    String code,
    long roleCount,
    long menuCount,
    String riskLevel,
    List<String> warnings
) {
}
