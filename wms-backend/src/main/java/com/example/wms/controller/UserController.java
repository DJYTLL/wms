package com.example.wms.controller;

import com.example.wms.audit.DeleteAuditScope;
import com.example.wms.dto.ApiResponse;
import com.example.wms.dto.DeleteRequest;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.UserCreateRequest;
import com.example.wms.dto.UserPasswordChangeRequest;
import com.example.wms.dto.UserPasswordResetRequest;
import com.example.wms.dto.UserResponse;
import com.example.wms.dto.UserRoleUpdateRequest;
import com.example.wms.dto.UserStatusUpdateRequest;
import com.example.wms.dto.UserUpdateRequest;
import com.example.wms.dto.RoleOptionResponse;
import com.example.wms.entity.Role;
import com.example.wms.exception.NotFoundException;
import com.example.wms.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// 用户管理接口
@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // 查询用户列表
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_user:view')")
    public ResponseEntity<ApiResponse<List<UserResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(userService.listAll()));
    }

    // 分页查询用户列表
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('PERM_user:view')")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> page(@RequestParam(defaultValue = "1") long page,
                                                                        @RequestParam(defaultValue = "20") long size,
                                                                        @RequestParam(required = false) String keyword,
                                                                        @RequestParam(required = false) Boolean enabled) {
        return ResponseEntity.ok(ApiResponse.ok(userService.page(page, size, keyword, enabled)));
    }

    // 查询用户详情
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_user:view')")
    public ResponseEntity<ApiResponse<UserResponse>> get(@PathVariable Long id) {
        UserResponse user = userService.getById(id);
        if (user == null) {
            throw new NotFoundException("用户不存在");
        }
        return ResponseEntity.ok(ApiResponse.ok(user));
    }

    // 新增用户
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_user:add')")
    public ResponseEntity<ApiResponse<UserResponse>> create(@Valid @RequestBody UserCreateRequest request) {
        UserResponse created = userService.create(request);
        return ResponseEntity.ok(ApiResponse.ok(created));
    }

    // 更新用户
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_user:edit')")
    public ResponseEntity<ApiResponse<UserResponse>> update(@PathVariable Long id,
                                                            @Valid @RequestBody UserUpdateRequest request) {
        UserResponse updated = userService.update(id, request);
        return ResponseEntity.ok(ApiResponse.ok(updated));
    }

    // 删除用户
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_user:delete')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id,
                                                    @Valid @RequestBody DeleteRequest request) {
        try (DeleteAuditScope ignored = DeleteAuditScope.bind(request.reason())) {
            userService.delete(id);
        }
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // 更新用户状态
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('PERM_user:edit')")
    public ResponseEntity<ApiResponse<Void>> updateStatus(@PathVariable Long id,
                                                          @RequestBody UserStatusUpdateRequest request) {
        userService.updateStatus(id, request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // 修改用户密码
    @PutMapping("/{id}/password")
    @PreAuthorize("hasAuthority('PERM_user:edit')")
    public ResponseEntity<ApiResponse<Void>> changePassword(@PathVariable Long id,
                                                            @Valid @RequestBody UserPasswordChangeRequest request) {
        userService.changePassword(id, request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // 重置用户密码
    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasAuthority('PERM_user:edit')")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@PathVariable Long id,
                                                           @Valid @RequestBody UserPasswordResetRequest request) {
        userService.resetPassword(id, request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // 查询用户角色
    @GetMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('PERM_user:view')")
    public ResponseEntity<ApiResponse<List<Role>>> listRoles(@PathVariable Long id) {
        List<Role> roles = userService.listRoles(id);
        return ResponseEntity.ok(ApiResponse.ok(roles));
    }

    // 查询角色下拉选项（用于用户管理）
    @GetMapping("/role-options")
    @PreAuthorize("hasAuthority('PERM_role:assign:view')")
    public ResponseEntity<ApiResponse<List<RoleOptionResponse>>> roleOptions() {
        return ResponseEntity.ok(ApiResponse.ok(userService.listRoleOptions()));
    }

    // 设置用户角色
    @PutMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('PERM_user:edit')")
    public ResponseEntity<ApiResponse<Void>> setRoles(@PathVariable Long id,
                                                      @Valid @RequestBody UserRoleUpdateRequest request) {
        userService.setRoles(id, request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
