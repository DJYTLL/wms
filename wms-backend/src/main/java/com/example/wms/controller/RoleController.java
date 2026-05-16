package com.example.wms.controller;

import com.example.wms.audit.DeleteAuditScope;
import com.example.wms.dto.ApiResponse;
import com.example.wms.dto.DeleteRequest;
import com.example.wms.dto.RoleColumnSettingResponse;
import com.example.wms.dto.RoleColumnPermissionUpdateRequest;
import com.example.wms.dto.RoleCreateRequest;
import com.example.wms.dto.RolePermissionUpdateRequest;
import com.example.wms.dto.RoleUpdateRequest;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.RoleOptionResponse;
import com.example.wms.entity.Permission;
import com.example.wms.entity.Role;
import com.example.wms.exception.NotFoundException;
import com.example.wms.service.RolePermissionService;
import com.example.wms.service.RoleService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// 角色管理接口
@RestController
@RequestMapping("/api/roles")
public class RoleController {
    private final RoleService roleService;
    private final RolePermissionService rolePermissionService;

    public RoleController(RoleService roleService,
                          RolePermissionService rolePermissionService) {
        this.roleService = roleService;
        this.rolePermissionService = rolePermissionService;
    }

    // 查询角色列表
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_role:view')")
    public ResponseEntity<ApiResponse<List<Role>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(roleService.listAll()));
    }

    // 查询角色下拉选项（列权限配置）
    @GetMapping("/options")
    @PreAuthorize("hasAuthority('PERM_column:role:manage')")
    public ResponseEntity<ApiResponse<List<RoleOptionResponse>>> listOptions() {
        List<RoleOptionResponse> options = roleService.listAll().stream()
            .filter(Role::isEnabled)
            .map(role -> new RoleOptionResponse(role.getId(), role.getCode(), role.getName()))
            .toList();
        return ResponseEntity.ok(ApiResponse.ok(options));
    }

    // 分页查询角色列表
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('PERM_role:view')")
    public ResponseEntity<ApiResponse<PageResponse<Role>>> page(@RequestParam(defaultValue = "1") long page,
                                                                @RequestParam(defaultValue = "20") long size,
                                                                @RequestParam(required = false) String keyword,
                                                                @RequestParam(required = false) Boolean enabled) {
        return ResponseEntity.ok(ApiResponse.ok(roleService.page(page, size, keyword, enabled)));
    }

    // 按 ID 查询角色
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_role:view')")
    public ResponseEntity<ApiResponse<Role>> get(@PathVariable Long id) {
        Role role = roleService.getById(id);
        if (role == null) {
            throw new NotFoundException("角色不存在");
        }
        return ResponseEntity.ok(ApiResponse.ok(role));
    }

    // 新增角色
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_role:add')")
    public ResponseEntity<ApiResponse<Role>> create(@Valid @RequestBody RoleCreateRequest request) {
        Role created = roleService.create(request);
        return ResponseEntity.ok(ApiResponse.ok(created));
    }

    // 更新角色
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_role:edit')")
    public ResponseEntity<ApiResponse<Role>> update(@PathVariable Long id,
                                                    @Valid @RequestBody RoleUpdateRequest request) {
        Role updated = roleService.update(id, request);
        return ResponseEntity.ok(ApiResponse.ok(updated));
    }

    // 删除角色
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_role:delete')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id,
                                                    @Valid @RequestBody DeleteRequest request) {
        try (DeleteAuditScope ignored = DeleteAuditScope.bind(request.reason())) {
            roleService.delete(id);
        }
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // 查询角色权限
    @GetMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('PERM_role:view')")
    public ResponseEntity<ApiResponse<List<Permission>>> listPermissions(@PathVariable Long id) {
        List<Permission> permissions = rolePermissionService.listPermissions(id);
        return ResponseEntity.ok(ApiResponse.ok(permissions));
    }

    // 查询角色列权限（列权限配置）
    @GetMapping("/{id}/column-permissions")
    @PreAuthorize("hasAuthority('PERM_column:role:manage')")
    public ResponseEntity<ApiResponse<List<Permission>>> listColumnPermissions(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(rolePermissionService.listColumnPermissions(id)));
    }

    // 查询角色页面列模板（列权限配置）
    @GetMapping("/{id}/column-settings/{pageKey}")
    @PreAuthorize("hasAuthority('PERM_column:role:manage')")
    public ResponseEntity<ApiResponse<RoleColumnSettingResponse>> getRoleColumnSetting(@PathVariable Long id,
                                                                                       @PathVariable String pageKey) {
        return ResponseEntity.ok(ApiResponse.ok(rolePermissionService.getRoleColumnSetting(id, pageKey)));
    }

    // 批量设置角色权限
    @PutMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('PERM_role:edit')")
    public ResponseEntity<ApiResponse<Void>> setPermissions(@PathVariable Long id,
                                                            @Valid @RequestBody RolePermissionUpdateRequest request) {
        rolePermissionService.setPermissions(id, request.permissionIds());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // 批量设置角色列权限（列权限配置）
    @PutMapping("/{id}/column-permissions")
    @PreAuthorize("hasAuthority('PERM_column:role:manage')")
    public ResponseEntity<ApiResponse<Void>> setColumnPermissions(@PathVariable Long id,
                                                                  @Valid @RequestBody RoleColumnPermissionUpdateRequest request) {
        rolePermissionService.setColumnPermissions(id, request.pageKey(), request.permissionIds());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // 追加单个权限
    @PostMapping("/{id}/permissions/{permissionId}")
    @PreAuthorize("hasAuthority('PERM_role:edit')")
    public ResponseEntity<ApiResponse<Void>> addPermission(@PathVariable Long id,
                                                           @PathVariable Long permissionId) {
        rolePermissionService.addPermission(id, permissionId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // 移除单个权限
    @DeleteMapping("/{id}/permissions/{permissionId}")
    @PreAuthorize("hasAuthority('PERM_role:edit')")
    public ResponseEntity<ApiResponse<Void>> removePermission(@PathVariable Long id,
                                                              @PathVariable Long permissionId,
                                                              @Valid @RequestBody DeleteRequest request) {
        try (DeleteAuditScope ignored = DeleteAuditScope.bind(request.reason())) {
            rolePermissionService.removePermission(id, permissionId);
        }
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
