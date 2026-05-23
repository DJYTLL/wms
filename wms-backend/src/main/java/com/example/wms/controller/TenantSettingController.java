package com.example.wms.controller;

import com.example.wms.dto.ApiResponse;
import com.example.wms.dto.TenantBusinessSettingsResponse;
import com.example.wms.dto.TenantBusinessSettingsUpdateRequest;
import com.example.wms.dto.TenantDisplaySettingsResponse;
import com.example.wms.dto.TenantDisplaySettingsUpdateRequest;
import com.example.wms.service.TenantSettingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 租户设置接口
@RestController
@RequestMapping("/api/tenant-settings")
public class TenantSettingController {
    private final TenantSettingService tenantSettingService;

    public TenantSettingController(TenantSettingService tenantSettingService) {
        this.tenantSettingService = tenantSettingService;
    }

    @GetMapping("/display")
    @PreAuthorize("hasAuthority('PERM_tenant-setting:view')")
    public ResponseEntity<ApiResponse<TenantDisplaySettingsResponse>> getDisplaySettings() {
        return ResponseEntity.ok(ApiResponse.ok(tenantSettingService.getDisplaySettings()));
    }

    @PutMapping("/display")
    @PreAuthorize("hasAuthority('PERM_tenant-setting:edit')")
    public ResponseEntity<ApiResponse<TenantDisplaySettingsResponse>> updateDisplaySettings(
        @RequestBody TenantDisplaySettingsUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(tenantSettingService.updateDisplaySettings(request)));
    }

    @GetMapping("/business")
    @PreAuthorize("hasAuthority('PERM_tenant-setting:view')")
    public ResponseEntity<ApiResponse<TenantBusinessSettingsResponse>> getBusinessSettings() {
        return ResponseEntity.ok(ApiResponse.ok(tenantSettingService.getBusinessSettings()));
    }

    @PutMapping("/business")
    @PreAuthorize("hasAuthority('PERM_tenant-setting:edit')")
    public ResponseEntity<ApiResponse<TenantBusinessSettingsResponse>> updateBusinessSettings(
        @RequestBody TenantBusinessSettingsUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(tenantSettingService.updateBusinessSettings(request)));
    }
}
