package com.example.wms.controller;

import com.example.wms.audit.DeleteAuditScope;
import com.example.wms.dto.ApiResponse;
import com.example.wms.dto.DeleteRequest;
import com.example.wms.dto.TenantCreateRequest;
import com.example.wms.dto.TenantMenuResponse;
import com.example.wms.dto.TenantMenuUpdateRequest;
import com.example.wms.dto.TenantResponse;
import com.example.wms.dto.TenantStatusUpdateRequest;
import com.example.wms.dto.TenantSwitchRequest;
import com.example.wms.dto.TenantUpdateRequest;
import com.example.wms.dto.TenantColumnSettingRequest;
import com.example.wms.dto.TenantColumnSettingResponse;
import com.example.wms.dto.TokenPairResponse;
import com.example.wms.exception.NotFoundException;
import com.example.wms.service.MenuService;
import com.example.wms.service.TenantService;
import com.example.wms.service.TenantColumnSettingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// 租户管理接口
@RestController
@RequestMapping("/api/tenants")
public class TenantController {
    private final TenantService tenantService;
    private final MenuService menuService;
    private final TenantColumnSettingService tenantColumnSettingService;

    public TenantController(TenantService tenantService,
                            MenuService menuService,
                            TenantColumnSettingService tenantColumnSettingService) {
        this.tenantService = tenantService;
        this.menuService = menuService;
        this.tenantColumnSettingService = tenantColumnSettingService;
    }

    // 查询租户列表（超级管理员）
    @GetMapping
    @PreAuthorize("hasRole('super_admin')")
    public ResponseEntity<ApiResponse<List<TenantResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(tenantService.listAll()));
    }

    // 查询租户详情（超级管理员）
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('super_admin')")
    public ResponseEntity<ApiResponse<TenantResponse>> get(@PathVariable Long id) {
        TenantResponse tenant = tenantService.getById(id);
        if (tenant == null) {
            throw new NotFoundException("租户不存在");
        }
        return ResponseEntity.ok(ApiResponse.ok(tenant));
    }

    // 新增租户（超级管理员）
    @PostMapping
    @PreAuthorize("hasRole('super_admin')")
    public ResponseEntity<ApiResponse<TenantResponse>> create(@Valid @RequestBody TenantCreateRequest request) {
        TenantResponse created = tenantService.create(request);
        return ResponseEntity.ok(ApiResponse.ok(created));
    }

    // 启用/停用租户（超级管理员）
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('super_admin')")
    public ResponseEntity<ApiResponse<TenantResponse>> updateStatus(@PathVariable Long id,
                                                                    @Valid @RequestBody TenantStatusUpdateRequest request) {
        TenantResponse updated = tenantService.updateStatus(id, request);
        return ResponseEntity.ok(ApiResponse.ok(updated));
    }

    // 修改租户名称（超级管理员）
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('super_admin')")
    public ResponseEntity<ApiResponse<TenantResponse>> updateName(@PathVariable Long id,
                                                                  @Valid @RequestBody TenantUpdateRequest request) {
        TenantResponse updated = tenantService.updateName(id, request);
        return ResponseEntity.ok(ApiResponse.ok(updated));
    }

    // 删除租户（软删除，超级管理员）
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('super_admin')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id,
                                                    @Valid @RequestBody DeleteRequest request) {
        try (DeleteAuditScope ignored = DeleteAuditScope.bind(request.reason())) {
            tenantService.delete(id);
        }
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // 切换租户（超级管理员）
    @PostMapping("/switch")
    @PreAuthorize("hasRole('super_admin')")
    public ResponseEntity<ApiResponse<TokenPairResponse>> switchTenant(@Valid @RequestBody TenantSwitchRequest request,
                                                                       HttpServletRequest httpRequest,
                                                                       HttpServletResponse httpResponse) {
        TokenPairResponse tokens = tenantService.switchTenant(request, httpRequest.getHeader(HttpHeaders.AUTHORIZATION));
        ResponseCookie cookie = ResponseCookie.from("refreshToken", tokens.refreshToken())
            .httpOnly(true)
            .secure(httpRequest.isSecure())
            .path("/api")
            .sameSite("Strict")
            .maxAge(java.time.Duration.ofDays(7))
            .build();
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.ok(ApiResponse.ok(new TokenPairResponse(tokens.token(), null, tokens.authPayload())));
    }

    // 查询租户菜单配置（超级管理员）
    @GetMapping("/{id}/menus")
    @PreAuthorize("hasRole('super_admin')")
    public ResponseEntity<ApiResponse<List<TenantMenuResponse>>> listTenantMenus(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(menuService.listTenantMenus(id)));
    }

    // 更新租户菜单配置（超级管理员）
    @PutMapping("/{id}/menus")
    @PreAuthorize("hasRole('super_admin')")
    public ResponseEntity<ApiResponse<Void>> updateTenantMenus(@PathVariable Long id,
                                                              @Valid @RequestBody TenantMenuUpdateRequest request) {
        menuService.updateTenantMenus(id, request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // 查询租户列配置（超级管理员）
    @GetMapping("/{id}/columns/{pageKey}")
    @PreAuthorize("hasRole('super_admin')")
    public ResponseEntity<ApiResponse<TenantColumnSettingResponse>> getTenantColumns(@PathVariable Long id,
                                                                                     @PathVariable String pageKey) {
        return ResponseEntity.ok(ApiResponse.ok(tenantColumnSettingService.getByTenantAndPageKey(id, pageKey)));
    }

    // 更新租户列配置（超级管理员）
    @PutMapping("/{id}/columns/{pageKey}")
    @PreAuthorize("hasRole('super_admin')")
    public ResponseEntity<ApiResponse<TenantColumnSettingResponse>> updateTenantColumns(@PathVariable Long id,
                                                                                       @PathVariable String pageKey,
                                                                                       @Valid @RequestBody TenantColumnSettingRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(tenantColumnSettingService.updateForTenant(id, pageKey, request)));
    }
}
