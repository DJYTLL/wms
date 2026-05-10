package com.example.wms.controller;

import com.example.wms.audit.DeleteAuditScope;
import com.example.wms.dto.ApiResponse;
import com.example.wms.dto.DeleteRequest;
import com.example.wms.dto.PermissionCreateRequest;
import com.example.wms.dto.PermissionUpdateRequest;
import com.example.wms.dto.PageResponse;
import com.example.wms.entity.Permission;
import com.example.wms.exception.NotFoundException;
import com.example.wms.service.PermissionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// 权限管理接口
@RestController
@RequestMapping("/api/permissions")
public class PermissionController {
    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    // 查询权限列表
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_role:view')")
    public ResponseEntity<ApiResponse<List<Permission>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(permissionService.listAll()));
    }

    // 分页查询权限列表
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('PERM_role:view')")
    public ResponseEntity<ApiResponse<PageResponse<Permission>>> page(@RequestParam(defaultValue = "1") long page,
                                                                      @RequestParam(defaultValue = "20") long size,
                                                                      @RequestParam(required = false) String keyword,
                                                                      @RequestParam(required = false) Boolean enabled) {
        return ResponseEntity.ok(ApiResponse.ok(permissionService.page(page, size, keyword, enabled)));
    }

    // 查询列权限列表（用于列权限配置）
    @GetMapping("/columns")
    @PreAuthorize("hasAuthority('PERM_column:role:manage')")
    public ResponseEntity<ApiResponse<List<Permission>>> listColumnPermissions() {
        return ResponseEntity.ok(ApiResponse.ok(permissionService.listColumnPermissions()));
    }

    // 按 ID 查询权限
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_role:view')")
    public ResponseEntity<ApiResponse<Permission>> get(@PathVariable Long id) {
        Permission permission = permissionService.getById(id);
        if (permission == null) {
            throw new NotFoundException("权限不存在");
        }
        return ResponseEntity.ok(ApiResponse.ok(permission));
    }

    // 新增权限
    @PostMapping
    @PreAuthorize("hasRole('super_admin')")
    public ResponseEntity<ApiResponse<Permission>> create(@Valid @RequestBody PermissionCreateRequest request) {
        Permission created = permissionService.create(request);
        return ResponseEntity.ok(ApiResponse.ok(created));
    }

    // 更新权限
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('super_admin')")
    public ResponseEntity<ApiResponse<Permission>> update(@PathVariable Long id,
                                                          @Valid @RequestBody PermissionUpdateRequest request) {
        Permission updated = permissionService.update(id, request);
        return ResponseEntity.ok(ApiResponse.ok(updated));
    }

    // 删除权限
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('super_admin')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id,
                                                    @Valid @RequestBody DeleteRequest request) {
        try (DeleteAuditScope ignored = DeleteAuditScope.bind(request.reason())) {
            permissionService.delete(id);
        }
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
