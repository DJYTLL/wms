package com.example.wms.dto;

public record RoleResponse(
    Long id,
    String code,
    String name,
    String description,
    boolean enabled,
    RoleCapabilitiesResponse capabilities
) {
}
