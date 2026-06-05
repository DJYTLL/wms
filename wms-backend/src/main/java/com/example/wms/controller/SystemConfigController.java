package com.example.wms.controller;

import com.example.wms.dto.ApiResponse;
import com.example.wms.dto.SystemConfigRequest;
import com.example.wms.dto.SystemConfigResponse;
import com.example.wms.service.SystemConfigService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// 系统配置接口
@RestController
@RequestMapping("/api/system-configs")
public class SystemConfigController {
    private final SystemConfigService systemConfigService;

    public SystemConfigController(SystemConfigService systemConfigService) {
        this.systemConfigService = systemConfigService;
    }

    // 查询全部配置（仅超级管理员）
    @GetMapping
    @PreAuthorize("hasRole('super_admin')")
    public ResponseEntity<ApiResponse<List<SystemConfigResponse>>> listAll() {
        return ResponseEntity.ok(ApiResponse.ok(systemConfigService.listAll()));
    }

    // 查询公开配置（无需认证）
    @GetMapping("/public")
    public ResponseEntity<ApiResponse<List<SystemConfigResponse>>> listPublic() {
        return ResponseEntity.ok(ApiResponse.ok(systemConfigService.listPublic()));
    }

    // 查询配置详情（仅超级管理员）
    @GetMapping("/{key}")
    @PreAuthorize("hasRole('super_admin') or @systemConfigPermissionEvaluator.canView(#key)")
    public ResponseEntity<ApiResponse<SystemConfigResponse>> getByKey(@PathVariable String key) {
        return ResponseEntity.ok(ApiResponse.ok(systemConfigService.getByKey(key)));
    }

    // 新增配置（仅超级管理员）
    @PostMapping("/{key}")
    @PreAuthorize("hasRole('super_admin')")
    public ResponseEntity<ApiResponse<SystemConfigResponse>> create(@PathVariable String key,
                                                                    @RequestBody SystemConfigRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(systemConfigService.create(key, request)));
    }

    // 更新配置（仅超级管理员）
    @PutMapping("/{key}")
    @PreAuthorize("hasRole('super_admin') or @systemConfigPermissionEvaluator.canEdit(#key)")
    public ResponseEntity<ApiResponse<SystemConfigResponse>> update(@PathVariable String key,
                                                                    @RequestBody SystemConfigRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(systemConfigService.update(key, request)));
    }
}
