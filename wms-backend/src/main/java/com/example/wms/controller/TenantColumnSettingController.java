package com.example.wms.controller;

import com.example.wms.dto.ApiResponse;
import com.example.wms.dto.TenantColumnSettingRequest;
import com.example.wms.dto.TenantColumnSettingResponse;
import com.example.wms.service.TenantColumnSettingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

// 租户列配置接口
@RestController
@RequestMapping("/api/tenant-columns")
public class TenantColumnSettingController {
    private final TenantColumnSettingService tenantColumnSettingService;

    public TenantColumnSettingController(TenantColumnSettingService tenantColumnSettingService) {
        this.tenantColumnSettingService = tenantColumnSettingService;
    }

    // 查询租户列配置
    @GetMapping("/{pageKey}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<TenantColumnSettingResponse>> get(@PathVariable String pageKey) {
        return ResponseEntity.ok(ApiResponse.ok(tenantColumnSettingService.getByPageKey(pageKey)));
    }

    // 更新租户列配置（租户管理员）
    @PutMapping("/{pageKey}")
    @PreAuthorize("hasAuthority('PERM_column:edit')")
    public ResponseEntity<ApiResponse<TenantColumnSettingResponse>> update(@PathVariable String pageKey,
                                                                           @RequestBody TenantColumnSettingRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(tenantColumnSettingService.update(pageKey, request)));
    }
}
