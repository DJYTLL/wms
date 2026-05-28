package com.example.wms.dto;

public record RoleCapabilitiesResponse(
    boolean canEdit,
    boolean canDelete,
    boolean canEditPermissions,
    boolean canManageColumnPermissions,
    String editDisabledReason,
    String deleteDisabledReason
) {
}
